/** Cartografia d'Emergenza: un suono scarno, nautico e reattivo allo stato della fuga. */
export type AudioCue = "start" | "lane" | "boost" | "silent" | "intel" | "fuel" | "impact" | "threat" | "fork" | "extract" | "fail";
type MusicState = "none" | "route" | "pursuit" | "uplink";

const MUSIC_TRACKS: Record<Exclude<MusicState, "none">, string> = {
  route: "/manus-storage/marea-nera/audio/marea-nera-route-bed_caaa6ee8.mp3",
  pursuit: "/manus-storage/marea-nera/audio/marea-nera-pursuit-bed_86441fda.mp3",
  uplink: "/manus-storage/marea-nera/audio/marea-nera-uplink-bed_2fbb7fea.mp3",
};

export class AudioDirector {
  private context: AudioContext | null = null;
  private motor: OscillatorNode | null = null;
  private motorGain: GainNode | null = null;
  private muted = false;
  private activeCue = "";
  private lastRadioAt = 0;
  private music = new Map<Exclude<MusicState, "none">, HTMLAudioElement>();
  private musicState: MusicState = "none";
  private radioDuckingUntil = 0;
  private cachedUrls: string[] = [];
  private readonly cacheName = "marea-nera-soundtrack-v1";
  private musicLevel = this.loadLevel("marea-nera-music-level-v2", .72);
  private sfxLevel = this.loadLevel("marea-nera-sfx-level-v2", .78);
  private onMix = (event: Event) => {
    const detail = (event as CustomEvent<{ music?: number; sfx?: number }>).detail;
    if (typeof detail?.music === "number") { this.musicLevel = Math.max(0, Math.min(1, detail.music)); localStorage.setItem("marea-nera-music-level-v2", String(this.musicLevel)); }
    if (typeof detail?.sfx === "number") { this.sfxLevel = Math.max(0, Math.min(1, detail.sfx)); localStorage.setItem("marea-nera-sfx-level-v2", String(this.sfxLevel)); }
    this.mixMusic();
  };

  constructor() { window.addEventListener("marea-nera-mix", this.onMix); }

  private loadLevel(key: string, fallback: number) {
    const stored = localStorage.getItem(key);
    if (stored === null) return fallback;
    const value = Number(stored);
    return Number.isFinite(value) ? Math.max(0, Math.min(1, value)) : fallback;
  }

  unlock() {
    this.ensureMusic();
    if (!this.context) {
      const WindowAudio = window.AudioContext || (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
      if (!WindowAudio) return;
      this.context = new WindowAudio();
      this.motor = this.context.createOscillator();
      this.motor.type = "sawtooth";
      this.motor.frequency.value = 46;
      this.motorGain = this.context.createGain();
      this.motorGain.gain.value = 0.0001;
      this.motor.connect(this.motorGain).connect(this.context.destination);
      this.motor.start();
    }
    void this.context.resume();
  }

  setMuted(muted: boolean) {
    this.muted = muted;
    if (muted) {
      window.speechSynthesis?.cancel();
      this.music.forEach((track) => { track.pause(); track.volume = 0; });
    } else if (this.musicState !== "none") {
      const activeTrack = this.music.get(this.musicState);
      if (activeTrack?.paused) void activeTrack.play().catch(() => undefined);
    }
    this.motorGain?.gain.setTargetAtTime(muted ? 0.0001 : .025 * this.sfxLevel, this.context?.currentTime ?? 0, .05);
    this.mixMusic();
  }

  tick(state: { boost: boolean; silent: boolean; detection: number; active: boolean; extract?: boolean }) {
    if (this.context && this.motor && this.motorGain) {
      const targetGain = (!state.active || this.muted ? 0.0001 : state.silent ? 0.007 : state.boost ? 0.044 : 0.024) * this.sfxLevel;
      const targetFrequency = state.silent ? 38 : state.boost ? 94 : 49 + state.detection * 13;
      this.motor.frequency.setTargetAtTime(targetFrequency, this.context.currentTime, .08);
      this.motorGain.gain.setTargetAtTime(targetGain, this.context.currentTime, .09);
    }
    const nextMusic: MusicState = !state.active ? "none" : state.extract ? "uplink" : state.detection > .64 ? "pursuit" : "route";
    this.setMusicState(nextMusic);
    this.mixMusic();
  }

  private ensureMusic() {
    if (this.music.size) return;
    (Object.entries(MUSIC_TRACKS) as [Exclude<MusicState, "none">, string][]).forEach(([state, source]) => {
      const track = new Audio(source);
      track.loop = true;
      track.preload = "auto";
      track.volume = 0;
      this.music.set(state, track);
      void this.cacheTrack(state, source, track);
    });
  }

  private async cacheTrack(state: Exclude<MusicState, "none">, source: string, track: HTMLAudioElement) {
    if (Capacitor.isNativePlatform()) {
      await this.cacheNativeTrack(state, source, track);
      return;
    }
    if (!("caches" in window)) return;
    try {
      const cache = await window.caches.open(this.cacheName);
      let response = await cache.match(source);
      if (!response) {
        response = await fetch(source, { credentials: "same-origin" });
        if (!response.ok) return;
        await cache.put(source, response.clone());
      }
      const blob = await response.blob();
      const localUrl = URL.createObjectURL(blob);
      const wasPlaying = !track.paused;
      const time = track.currentTime;
      track.src = localUrl;
      this.cachedUrls.push(localUrl);
      if (wasPlaying) { track.currentTime = time; void track.play().catch(() => undefined); }
    } catch { /* La musica continua dal percorso File Storage quando la cache non è disponibile. */ }
  }

  private async cacheNativeTrack(state: Exclude<MusicState, "none">, source: string, track: HTMLAudioElement) {
    const path = `marea-nera-audio/${state}.mp3`;
    try {
      await Filesystem.stat({ path, directory: Directory.Cache });
      const cached = await Filesystem.getUri({ path, directory: Directory.Cache });
      this.switchToCachedUrl(track, Capacitor.convertFileSrc(cached.uri));
      return;
    } catch { /* Primo avvio: scarica l'asset dal File Storage. */ }
    try {
      const downloaded = await fetch(source, { credentials: "same-origin" });
      if (!downloaded.ok) return;
      const base64 = this.toBase64(await downloaded.arrayBuffer());
      await Filesystem.writeFile({ path, data: base64, directory: Directory.Cache, recursive: true });
      const cached = await Filesystem.getUri({ path, directory: Directory.Cache });
      this.switchToCachedUrl(track, Capacitor.convertFileSrc(cached.uri));
    } catch { /* Resta attivo il percorso File Storage in caso di rete o filesystem non disponibili. */ }
  }

  private toBase64(buffer: ArrayBuffer) {
    const bytes = new Uint8Array(buffer);
    let binary = "";
    for (let i = 0; i < bytes.length; i += 8192) {
      const end = Math.min(i + 8192, bytes.length);
      for (let index = i; index < end; index += 1) binary += String.fromCharCode(bytes[index]);
    }
    return btoa(binary);
  }

  private switchToCachedUrl(track: HTMLAudioElement, localUrl: string) {
    const wasPlaying = !track.paused;
    const time = track.currentTime;
    track.src = localUrl;
    if (wasPlaying) { track.currentTime = time; void track.play().catch(() => undefined); }
  }

  private setMusicState(next: MusicState) {
    if (this.musicState === next) return;
    this.musicState = next;
    if (next === "none") {
      this.music.forEach((track) => { track.pause(); track.volume = 0; });
      return;
    }
    const track = this.music.get(next);
    if (track && track.paused && !this.muted) void track.play().catch(() => undefined);
  }

  private mixMusic() {
    const ducked = performance.now() < this.radioDuckingUntil;
    this.music.forEach((track, state) => {
      const target = (this.muted || state !== this.musicState ? 0 : ducked ? .07 : state === "pursuit" ? .22 : state === "uplink" ? .18 : .15) * this.musicLevel;
      track.volume += (target - track.volume) * .11;
      if (state !== this.musicState && track.volume < .003 && !track.paused) track.pause();
    });
  }

  cue(kind: AudioCue) {
    if (!this.context || this.muted) return;
    const now = this.context.currentTime;
    const recipes: Record<AudioCue, [number, number, number, OscillatorType]> = {
      start: [180, 360, .10, "sine"], lane: [280, 340, .045, "sine"], boost: [110, 220, .075, "sawtooth"], silent: [180, 100, .06, "triangle"],
      intel: [490, 740, .12, "sine"], fuel: [175, 260, .10, "triangle"], impact: [118, 42, .16, "square"], threat: [200, 92, .17, "sawtooth"],
      fork: [360, 520, .11, "sine"], extract: [330, 660, .22, "sine"], fail: [160, 44, .28, "sawtooth"],
    };
    const [from, to, duration, wave] = recipes[kind];
    const oscillator = this.context.createOscillator();
    const gain = this.context.createGain();
    oscillator.type = wave;
    oscillator.frequency.setValueAtTime(from, now);
    oscillator.frequency.exponentialRampToValueAtTime(Math.max(25, to), now + duration);
    gain.gain.setValueAtTime(.0001, now);
    gain.gain.exponentialRampToValueAtTime((kind === "impact" || kind === "threat" ? .13 : .08) * this.sfxLevel, now + .012);
    gain.gain.exponentialRampToValueAtTime(.0001, now + duration);
    oscillator.connect(gain).connect(this.context.destination);
    oscillator.start(now);
    oscillator.stop(now + duration + .03);
  }

  radio(line: string, urgent = false) {
    if (this.muted || !line || !window.speechSynthesis) return;
    const now = performance.now();
    if (!urgent && now - this.lastRadioAt < 5500) return;
    this.lastRadioAt = now;
    this.radioDuckingUntil = now + (urgent ? 5200 : 4200);
    this.mixMusic();
    const content = line.replace(/^(MAYA|KROSS):\s*/i, "").replace(/…/g, "");
    const utterance = new SpeechSynthesisUtterance(content);
    utterance.lang = "it-IT";
    utterance.rate = urgent ? .98 : .86;
    utterance.pitch = /^KROSS:/i.test(line) ? .73 : 1.08;
    utterance.volume = .68;
    const italian = window.speechSynthesis.getVoices().find((voice) => voice.lang.toLowerCase().startsWith("it"));
    if (italian) utterance.voice = italian;
    utterance.onend = () => { this.radioDuckingUntil = 0; };
    utterance.onerror = () => { this.radioDuckingUntil = 0; };
    this.cue("lane");
    window.speechSynthesis.speak(utterance);
  }

  cueState(state: "boost" | "silent") {
    if (this.activeCue === state) return;
    this.activeCue = state;
    this.cue(state);
  }

  resetState() { this.activeCue = ""; }

  dispose() {
    window.speechSynthesis?.cancel();
    this.motor?.stop();
    window.removeEventListener("marea-nera-mix", this.onMix);
    this.music.forEach((track) => { track.pause(); track.removeAttribute("src"); track.load(); });
    this.music.clear();
    this.cachedUrls.forEach((url) => URL.revokeObjectURL(url));
    this.cachedUrls = [];
    void this.context?.close();
  }
}
import { Capacitor } from "@capacitor/core";
import { Directory, Filesystem } from "@capacitor/filesystem";
