import { readFileSync, existsSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const root = resolve(import.meta.dirname, "..");
const audioDirector = readFileSync(resolve(root, "client/src/game/AudioDirector.ts"), "utf8");
const mixer = readFileSync(resolve(root, "client/src/components/AudioMixer.tsx"), "utf8");

describe("colonna sonora Marea Nera", () => {
  it("usa le tre risorse persistenti di File Storage", () => {
    expect(audioDirector).toContain("marea-nera-route-bed_caaa6ee8.mp3");
    expect(audioDirector).toContain("marea-nera-pursuit-bed_86441fda.mp3");
    expect(audioDirector).toContain("marea-nera-uplink-bed_2fbb7fea.mp3");
    expect(audioDirector).toContain("/manus-storage/marea-nera/audio/");
  });

  it("mantiene loop, ducking radio, mute e livelli indipendenti", () => {
    expect(audioDirector).toContain("track.loop = true");
    expect(audioDirector).toContain("radioDuckingUntil");
    expect(audioDirector).toContain("marea-nera-music-level-v2");
    expect(audioDirector).toContain("marea-nera-sfx-level-v2");
    expect(audioDirector).toContain("marea-nera-soundtrack-v1");
    expect(audioDirector).toContain("window.caches.open");
    expect(audioDirector).toContain("Filesystem.writeFile");
    expect(audioDirector).toContain("Directory.Cache");
    expect(audioDirector).toContain("Filesystem.stat");
  });

  it("espone slider accessibili per musica ed effetti", () => {
    expect(mixer).toContain('aria-label="Volume musica"');
    expect(mixer).toContain('aria-label="Volume effetti"');
    expect(mixer).toContain("marea-nera-mix");
    expect(mixer).toContain("stored === null");
    expect(audioDirector).toContain("if (stored === null) return fallback");
  });

  it("conserva le sorgenti audio nel pacchetto di produzione", () => {
    ["route", "pursuit", "uplink"].forEach((name) => {
      expect(existsSync(`/home/ubuntu/webdev-static-assets/marea-nera-${name}-bed.mp3`)).toBe(true);
    });
  });
});
