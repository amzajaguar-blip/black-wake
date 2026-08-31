/** Cartografia d'Emergenza: HUD sobrio, radio leggibile e dossier di missione. */
import { ASSETS, MODULES, type Chapter, type Modules, type Sector } from "./data";

export type HudSnapshot = {
  hull: number; fuel: number; intel: number; intelGoal: number; detection: number; sector: Sector; sectorElapsed: number;
  chapterCode: string; paused: boolean; muted: boolean; boost: boolean; silent: boolean; forkNotice: string | null; transmission: number | null; drift: number;
};

export class HudController {
  private root: HTMLDivElement;
  private screen: HTMLDivElement;
  private hud: HTMLDivElement;
  private toast: HTMLDivElement;
  private controls: HTMLDivElement;
  private lastHud = 0;

  constructor() {
    this.root = document.createElement("div");
    this.root.className = "mn-overlay";
    this.root.innerHTML = `<div class="mn-vignette"></div><div class="mn-rotate-device"><img src="${ASSETS.logo}" alt="" /><p>RUOTA IL DISPOSITIVO</p><span>Marea Nera richiede la rotta orizzontale.</span></div><div class="mn-hud" aria-live="polite"></div><div class="mn-toast" role="status"></div><div class="mn-controls" aria-label="Controlli touch"></div><div class="mn-screen"></div>`;
    document.body.appendChild(this.root);
    this.hud = this.root.querySelector(".mn-hud") as HTMLDivElement;
    this.toast = this.root.querySelector(".mn-toast") as HTMLDivElement;
    this.controls = this.root.querySelector(".mn-controls") as HTMLDivElement;
    this.screen = this.root.querySelector(".mn-screen") as HTMLDivElement;
  }

  private bindAction(id: string, callback: () => void) { this.screen.querySelector<HTMLElement>(`#${id}`)?.addEventListener("click", callback); }

  setTouchHandlers(handlers: { left: () => void; right: () => void; boost: (held: boolean) => void; silent: (held: boolean) => void; pause: () => void }) {
    this.controls.innerHTML = `<div class="mn-control-bank mn-steer-bank"><button id="mn-left" class="mn-control" aria-label="Sposta a sinistra">◀</button><button id="mn-right" class="mn-control" aria-label="Sposta a destra">▶</button></div><div class="mn-control-bank mn-action-bank"><button id="mn-silent" class="mn-control mn-silent" aria-label="Naviga in silenzio">SILENZIO</button><button id="mn-boost" class="mn-control mn-boost" aria-label="Attiva boost">BOOST</button><button id="mn-pause" class="mn-control mn-pause" aria-label="Pausa">Ⅱ</button></div>`;
    this.controls.querySelector<HTMLButtonElement>("#mn-left")?.addEventListener("click", handlers.left);
    this.controls.querySelector<HTMLButtonElement>("#mn-right")?.addEventListener("click", handlers.right);
    this.controls.querySelector<HTMLButtonElement>("#mn-pause")?.addEventListener("click", handlers.pause);
    const bindHold = (selector: string, handler: (held: boolean) => void) => {
      const button = this.controls.querySelector<HTMLButtonElement>(selector);
      if (!button) return;
      const down = (event: PointerEvent) => { event.preventDefault(); button.setPointerCapture?.(event.pointerId); button.classList.add("is-held"); handler(true); };
      const up = () => { button.classList.remove("is-held"); handler(false); };
      button.addEventListener("pointerdown", down); button.addEventListener("pointerup", up); button.addEventListener("pointercancel", up); button.addEventListener("pointerleave", up);
    };
    bindHold("#mn-boost", handlers.boost); bindHold("#mn-silent", handlers.silent);
  }

  showMenu(chapters: Chapter[], unlocked: number, onChapter: (index: number) => void, onGarage: () => void, bank: number, best: number) {
    this.hideGameplay();
    const cards = chapters.map((chapter, index) => {
      const open = index < unlocked;
      return `<article class="mn-chapter-card ${open ? "is-open" : "is-locked"}"><div><span>CAP. ${chapter.code}</span><strong>${chapter.title}</strong><p>${chapter.subtitle}</p></div>${open ? `<button data-chapter="${index}">APRI DOSSIER <i>→</i></button>` : `<em>SEGNALE BLOCCATO</em>`}</article>`;
    }).join("");
    this.screen.className = "mn-screen is-open mn-menu-screen";
    this.screen.innerHTML = `<div class="mn-menu-backdrop" style="background-image:linear-gradient(90deg, rgba(3,11,16,.96) 0%, rgba(3,11,16,.74) 44%, rgba(3,11,16,.25) 100%), url('${ASSETS.visualTarget}')"></div><div class="mn-menu-chart" aria-hidden="true"><i class="route-left"></i><i class="route-mid"></i><i class="route-right"></i><b>ROUTE 07.14N</b><span>34° 46' N<br/>12° 09' E</span></div><section class="mn-menu-content mn-campaign-menu"><div class="mn-brand-lockup"><img src="${ASSETS.logo}" alt="" /><div><span>OPERAZIONE // 07.14N / 12.09E</span><h1>MAREA<br/><em>NERA</em></h1></div></div><p class="mn-kicker">CAMPAGNA OPERATIVA // ${unlocked}/8 DOSSIER DECIFRATI</p><p class="mn-menu-copy">Ogni traccia apre una rotta, ogni rotta fa rumore. Scegli il dossier che riesci ancora a leggere.</p><div class="mn-campaign-grid">${cards}</div><div class="mn-menu-actions"><button id="mn-garage" class="mn-secondary">APRI OFFICINA</button></div><div class="mn-menu-stats"><span><b>${bank}</b> INTEL BANCATA</span><span><b>${best}</b> RECORD MISSIONE</span><span>RADIO ADATTIVA</span></div></section>`;
    this.screen.querySelectorAll<HTMLButtonElement>("[data-chapter]").forEach((button) => button.addEventListener("click", () => onChapter(Number(button.dataset.chapter))));
    this.bindAction("mn-garage", onGarage);
  }

  showBriefing(chapter: Chapter, onLaunch: () => void, onBack: () => void) {
    this.hideGameplay();
    const threshold = Math.ceil(chapter.intelGoal * .7);
    this.screen.className = "mn-screen is-open mn-briefing-screen";
    this.screen.innerHTML = `<div class="mn-briefing-image"></div><section class="mn-briefing-sheet"><div class="mn-section-code">CAPITOLO ${chapter.code} <span>///</span> BLACK TIDE</div><h2>${chapter.title}</h2><p class="mn-subtitle">${chapter.subtitle}</p><div class="mn-briefing-rule"></div><div class="mn-briefing-copy">${chapter.briefing.map((line) => `<p>${line}</p>`).join("")}</div><aside class="mn-radio-card"><span>CAN. 04 · MAYA SERRANO</span><p>“${chapter.maya}”</p></aside><div class="mn-objective"><span>OBIETTIVO</span><strong>Raccogli almeno ${threshold}/${chapter.intelGoal} INTEL e sopravvivi alla trasmissione.</strong></div><div class="mn-menu-actions"><button id="mn-launch" class="mn-primary">AVVIA FUGA <span>→</span></button><button id="mn-back" class="mn-secondary">TORNA AI DOSSIER</button></div></section>`;
    this.bindAction("mn-launch", onLaunch); this.bindAction("mn-back", onBack);
  }

  showGarage(modules: Modules, bank: number, onBuy: (id: keyof Modules) => void, onBack: () => void) {
    this.hideGameplay();
    this.screen.className = "mn-screen is-open mn-garage-screen";
    const modulesMarkup = MODULES.map((module) => {
      const level = modules[module.id]; const cost = level < 3 ? module.costs[level] : 0;
      return `<article class="mn-module ${level === 3 ? "is-max" : ""}"><div><p>${module.tag}</p><h3>${module.title}</h3><span>${module.detail}</span></div><div class="mn-module-right"><div class="mn-pips">${[0, 1, 2].map((n) => `<i class="${n < level ? "is-on" : ""}"></i>`).join("")}</div>${level < 3 ? `<button data-module="${module.id}" ${bank < cost ? "disabled" : ""}>${cost} INTEL</button>` : "<strong>MAX</strong>"}</div></article>`;
    }).join("");
    this.screen.innerHTML = `<section class="mn-garage-sheet"><div class="mn-section-code">RIFUGIO NON REGISTRATO <span>///</span> OFFICINA</div><h2>LA BARCA È LA<br/><em>SECONDA PROTAGONISTA.</em></h2><p class="mn-garage-bank"><b>${bank}</b> INTEL DISPONIBILE</p><div class="mn-modules">${modulesMarkup}</div><button id="mn-garage-back" class="mn-secondary">CHIUDI OFFICINA</button></section>`;
    this.screen.querySelectorAll<HTMLButtonElement>("[data-module]").forEach((button) => button.addEventListener("click", () => onBuy(button.dataset.module as keyof Modules)));
    this.bindAction("mn-garage-back", onBack);
  }

  showDebrief(result: { won: boolean; intel: number; banked: number; reason: string; peakSignal: number; chapter: Chapter; nextUnlocked: boolean }, onContinue: () => void, onRestart: () => void, onMenu: () => void) {
    this.hideGameplay();
    const { chapter } = result;
    const headline = result.won ? (result.nextUnlocked ? "DOSSIER DECIFRATO" : "MAREA NERA APERTA") : "SEGNALE INTERROTTO";
    const story = result.won ? chapter.closer.join(" ") : "La prova non è perduta. Ma BLACK TIDE ha guadagnato un'altra ora.";
    const primary = result.won && result.nextUnlocked ? "APRI PROSSIMO DOSSIER" : "RIPRENDI IL MARE";
    this.screen.className = "mn-screen is-open mn-debrief-screen";
    this.screen.innerHTML = `<section class="mn-debrief-sheet ${result.won ? "is-win" : "is-loss"}"><div class="mn-section-code">CAP. ${chapter.code} <span>///</span> ${result.won ? "ESTRATTO" : "COMPROMESSO"}</div><h2>${headline}</h2><p class="mn-debrief-reason">${result.reason}</p><div class="mn-debrief-metrics"><span><i>${result.intel}</i> INTEL RACCOLTA</span><span><i>+${result.banked}</i> INTEL BANCATA</span><span><i>${Math.round(result.peakSignal * 100)}%</i> PICCO SEGNALE</span></div><p class="mn-debrief-story">${story}</p><div class="mn-menu-actions"><button id="mn-continue" class="mn-primary">${primary} <span>→</span></button>${result.won ? "" : `<button id="mn-restart" class="mn-secondary">RIPROVA CAPITOLO</button>`}<button id="mn-debrief-menu" class="mn-secondary">TORNA AI DOSSIER</button></div></section>`;
    this.bindAction("mn-continue", onContinue); this.bindAction("mn-restart", onRestart); this.bindAction("mn-debrief-menu", onMenu);
  }

  showPause(onResume: () => void, onQuit: () => void) { this.screen.className = "mn-screen is-open mn-pause-screen"; this.screen.innerHTML = `<section class="mn-pause-card"><div class="mn-section-code">SISTEMA IN ATTESA</div><h2>ROTTA SOSPESA</h2><p>Il mare aspetta. BLACK TIDE no.</p><button id="mn-resume" class="mn-primary">TORNA IN ROTTA</button><button id="mn-quit" class="mn-secondary">ABBANDONA MISSIONE</button></section>`; this.bindAction("mn-resume", onResume); this.bindAction("mn-quit", onQuit); }
  hideScreen() { this.screen.className = "mn-screen"; this.screen.innerHTML = ""; }
  hideGameplay() { this.hud.classList.remove("is-visible"); this.controls.classList.remove("is-visible"); }
  showGameplay() { this.hideScreen(); this.hud.classList.add("is-visible"); this.controls.classList.add("is-visible"); }
  flash(text: string, tone: "cyan" | "amber" | "red" = "cyan") { this.toast.textContent = text; this.toast.className = `mn-toast is-visible is-${tone}`; window.setTimeout(() => { this.toast.className = "mn-toast"; }, 1600); }

  render(snapshot: HudSnapshot) {
    const now = performance.now(); if (now - this.lastHud < 80) return; this.lastHud = now;
    const progress = Math.min(100, (snapshot.sectorElapsed / snapshot.sector.duration) * 100); const signalClass = snapshot.detection > .88 ? "hot" : snapshot.detection > .64 ? "warn" : "";
    this.hud.classList.toggle("is-alert", snapshot.detection > .64);
    this.hud.classList.toggle("is-critical", snapshot.detection > .88);
    this.hud.innerHTML = `<div class="mn-hud-top"><div class="mn-status-stack"><div class="mn-brand-mini"><img src="${ASSETS.logo}" alt=""/> MAREA NERA <span>CH.${snapshot.chapterCode}</span></div>${this.meter("SCAFO", snapshot.hull, "hull")}${this.meter("CARBURANTE", snapshot.fuel, "fuel")}</div><div class="mn-radar ${signalClass}"><div class="mn-radar-sweep"></div><i class="p1"></i><i class="p2"></i><i class="p3"></i><b>RADAR</b></div><div class="mn-status-stack mn-right-stack"><div class="mn-metric"><span>INTEL</span><b>${snapshot.intel}<em>/${snapshot.intelGoal}</em></b></div><div class="mn-metric ${signalClass}"><span>SEGNATURA</span><b>${Math.round(snapshot.detection * 100)}<em>%</em></b></div><div class="mn-comms">COMMS ${snapshot.muted ? "OFF" : "ON"} · M</div></div></div><div class="mn-sector-card"><span>COORD. ${snapshot.sector.id.toUpperCase()} / ${snapshot.sector.tag}</span><strong>${snapshot.sector.title}</strong><div class="mn-sector-progress"><i style="width:${progress}%"></i></div></div>${snapshot.forkNotice ? `<div class="mn-fork-notice">${snapshot.forkNotice}</div>` : ""}${snapshot.transmission !== null ? `<div class="mn-transmission"><span>TRASMISSIONE</span><i><b style="width:${snapshot.transmission}%"></b></i><em>${Math.round(snapshot.transmission)}%</em></div>` : ""}<div class="mn-run-state">${snapshot.boost ? "BOOST // FIRMA IN AUMENTO" : snapshot.silent ? "SILENZIO // FIRMA IN CALO" : snapshot.drift > 0 ? `DERIVA // ${snapshot.drift.toFixed(1)}s` : "ROUTING // STABILE"}</div>`;
  }

  private meter(label: string, value: number, variant: string) { return `<div class="mn-meter ${variant}"><span>${label}</span><i><b style="width:${Math.max(0, Math.min(100, value))}%"></b></i><em>${Math.round(value)}%</em></div>`; }
  dispose() { this.root.remove(); }
}
