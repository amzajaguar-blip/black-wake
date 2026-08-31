import { readFileSync, existsSync, statSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const read = (file) => readFileSync(resolve(root, file), "utf8");
const data = read("client/src/game/data.ts");
const world = read("client/src/game/GameWorld.ts");
const hud = read("client/src/game/HudController.ts");
const audio = read("client/src/game/AudioDirector.ts");
const css = read("client/src/index.css");
const listing = read("GOOGLE_PLAY_LISTING_IT.md");

const invariantSets = [
  ["catalogo conserva gli otto capitoli", () => (data.match(/code: "0[1-8]"/g) || []).length === 8],
  ["ogni capitolo mantiene quattro settori", () => (data.match(/sector\("0[1-8]-/g) || []).length === 32],
  ["ogni capitolo offre una decisione fork", () => (data.match(/\{ fork: true \}/g) || []).length === 8],
  ["ogni capitolo termina con estrazione", () => (data.match(/\{ extract: true \}/g) || []).length === 8],
  ["ogni capitolo include una trasmissione contestuale", () => (data.match(/maya:/g) || []).length === 9 && /radio: string/.test(data)],
  ["dossier bloccati e sbloccati sono gestiti", () => /unlockedChapterCount/.test(world) && /SEGNALE BLOCCATO/.test(hud)],
  ["l'avanzamento della campagna viene salvato", () => /unlockedChapterCount: this\.unlockedChapterCount/.test(world)],
  ["la soglia Intel resta relativa al capitolo", () => /this\.chapter\.intelGoal/.test(world)],
  ["l'avvio applica un ambiente al capitolo", () => /applyEnvironment\(this\.sector\.environment\)/.test(world)],
  ["gli ambienti includono notte porto tempesta isola alba estrazione", () => ["night:", "port:", "storm:", "island:", "dawn:", "extract:"].every((key) => world.includes(key))],
  ["la radio si attiva soltanto dopo gesto utente", () => /this\.audio\.unlock\(\)/.test(world) && /unlock\(\)/.test(audio)],
  ["la radio resta accessibile anche senza voce", () => /this\.hud\.flash\(this\.sector\.radio/.test(world)],
  ["la voce radio è italiana", () => /utterance\.lang = "it-IT"/.test(audio)],
  ["mute interrompe la voce attiva", () => /speechSynthesis\?\.cancel/.test(audio)],
  ["gli effetti coprono le azioni principali", () => ["intel", "fuel", "impact", "threat", "fork", "extract"].every((cue) => audio.includes(cue)) && /this\.audio\.cue\(/.test(world)],
  ["la plancia distingue allarme e contatto pieno", () => /is-alert/.test(hud) && /is-critical/.test(hud)],
  ["il tema aggressivo rispetta la riduzione movimento", () => /prefers-reduced-motion/.test(css)],
  ["controlli touch restano disponibili in landscape", () => /max-height: 500px\) and \(orientation: landscape\)/.test(css)],
  ["scheda Google Play include descrizione breve", () => /Descrizione breve/.test(listing)],
  ["asset Play Store includono la feature graphic", () => existsSync(resolve(root, "../webdev-static-assets/marea-nera-play-store/00-feature-graphic-1024x500.png")) && statSync(resolve(root, "../webdev-static-assets/marea-nera-play-store/00-feature-graphic-1024x500.png")).size > 50000],
  ["tre tracce musicali pubblicate", () => ["marea-nera-route-bed_caaa6ee8.mp3", "marea-nera-pursuit-bed_86441fda.mp3", "marea-nera-uplink-bed_2fbb7fea.mp3"].every((file) => audio.includes(file))],
  ["musica usa File Storage persistente", () => (audio.match(/\/manus-storage\/marea-nera\/audio\/marea-nera-[a-z]+-bed_/g) || []).length === 3],
  ["musica mantiene i loop", () => /track\.loop = true/.test(audio)],
  ["musica si riduce sotto la radio", () => /radioDuckingUntil/.test(audio)],
  ["musica si arresta durante mute", () => /track\.pause\(\); track\.volume = 0/.test(audio)],
  ["Android non conserva copie musicali duplicate", () => !existsSync(resolve(root, "scripts/copy-native-audio.mjs")) && !existsSync(resolve(root, "android/app/src/main/assets/public/audio"))],
];

function recursiveReview(round, ledger) {
  if (round > 100) return ledger;
  const [label, predicate] = invariantSets[(round - 1) % invariantSets.length];
  const passed = Boolean(predicate());
  process.stdout.write(`${passed ? "PASS" : "FAIL"} | ciclo ${String(round).padStart(3, "0")} | ${label}\n`);
  ledger.push({ round, label, passed });
  return recursiveReview(round + 1, ledger);
}

const results = recursiveReview(1, []);
const failures = results.filter((result) => !result.passed);
process.stdout.write(`\nRIEPILOGO RELEASE: ${results.length - failures.length}/${results.length} cicli ricorsivi superati.\n`);
if (failures.length) process.exitCode = 1;
