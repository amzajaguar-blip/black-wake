import { useEffect, useState } from "react";
import { Slider } from "@/components/ui/slider";

function savedLevel(key: string, fallback: number) {
  const stored = localStorage.getItem(key);
  if (stored === null) return Math.round(fallback * 100);
  const value = Number(stored);
  return Number.isFinite(value) ? Math.round(Math.max(0, Math.min(1, value)) * 100) : Math.round(fallback * 100);
}

export default function AudioMixer() {
  const [music, setMusic] = useState(() => savedLevel("marea-nera-music-level-v2", .72));
  const [sfx, setSfx] = useState(() => savedLevel("marea-nera-sfx-level-v2", .78));
  useEffect(() => { window.dispatchEvent(new CustomEvent("marea-nera-mix", { detail: { music: music / 100 } })); }, [music]);
  useEffect(() => { window.dispatchEvent(new CustomEvent("marea-nera-mix", { detail: { sfx: sfx / 100 } })); }, [sfx]);

  return <aside className="mn-audio-mixer" aria-label="Mixer audio"><span>COMMS MIX</span><label>MUSICA <b>{music}%</b><Slider value={[music]} min={0} max={100} step={1} aria-label="Volume musica" onValueChange={(value) => setMusic(value[0] ?? 0)} /></label><label>EFFETTI <b>{sfx}%</b><Slider value={[sfx]} min={0} max={100} step={1} aria-label="Volume effetti" onValueChange={(value) => setSfx(value[0] ?? 0)} /></label><small>M DISATTIVA TUTTO</small></aside>;
}
