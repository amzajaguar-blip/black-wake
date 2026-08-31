# Black Wake

> An operational nautical thriller of escape and survival.
> English brand rebrand of **Marea Nera**.

**Black Wake** is a browser-based, lane-based chase game. You pilot Elias Vane through hostile waters on a three-lane sea, choosing between safe routes, intelligence rewards, and volatile shortcuts, while Black Tide patrols close the gaps.

This repository is the complete project: Babylon.js scene, React 19 UI, Capacitor Android shell, audio director, sector scheduler, HUD controller, and Play Store artifacts.

## Quickstart

```bash
# install dependencies (pnpm 10.x)
pnpm install

# run the dev server (port 3000)
pnpm dev

# build the static web bundle into dist/public/
pnpm build

# typecheck
pnpm check

# tests
pnpm test
```

Open `http://localhost:3000` once `pnpm dev` is up. The canvas mounts the game at `/`.

## Controls

| Input | Action |
| --- | --- |
| A / D, ← / → | Change lane (left / right) |
| W / Space | Boost (burns fuel, raises detection) |
| S / ↓ | Silent run (lowers detection, slows) |
| P / Esc | Pause |
| M | Mute audio |
| Touch | On-screen lane buttons, boost, silent, pause |

The game supports keyboard and touch. The touch layer activates on narrow landscape displays.

## Project layout

```
black-wake/
├── client/                  # Vite + React frontend
│   ├── src/game/            # Babylon scene, world, input, HUD, audio
│   ├── src/components/      # React UI shell
│   └── public/              # Static assets (manifest, logo, audio)
├── server/                  # Express backend (asset storage proxy)
├── shared/                  # Shared types
├── android/                 # Capacitor Android project
├── play-store/              # Play Store screenshots & feature graphic
├── media/                   # Original soundtrack + trailer
├── drizzle/                 # DB migrations
├── scripts/                 # Review and release scripts
├── dist/public/             # Built static bundle (AI Studio upload target)
└── capacitor.config.ts      # Capacitor app config (appName: Black Wake)
```

## Architecture

> React is the frame. Babylon is the sea. TypeScript classes are the escape.

- `components/GameCanvas.tsx` — single canvas lifecycle owner (no double-mount).
- `game/scene.ts` — Babylon scene, camera, lights, GUI, lifecycle handle.
- `game/GameWorld.ts` — mission state, sector loop, update, end conditions.
- `game/InputManager.ts` — keyboard and touch → semantic intents.
- `game/SectorDirector.ts` — sector data, forks, timer, extraction event, radio.
- `game/PlayerBoat.ts` — lane running, fuel, hull, detection, mesh.
- `game/EntityManager.ts` — threats and pickups: spawn, motion, collect, collide.
- `game/HudController.ts` — edge-bound HUD panels, labels, radar, radio lines.
- `game/data.ts` — chapters, sectors, environments, dialogue, modules.

See `STRUCTURE.md` for the full breakdown.

## Brand & rebrand notes

This repo was originally published under the Italian name **Marea Nera**. It is now the English-facing project **Black Wake**.

- Brand-layer files updated: `package.json`, `client/public/manifest.webmanifest`, `capacitor.config.ts`, top-level doc headers, `MEMORY.md`.
- Code identifiers (`mn-*` classes, CSS, game-internal strings) remain Italian. Game narrative and HUD copy are still Italian by design.
- The in-game faction **Black Tide** stays as-is. The brand **Black Wake** is the protagonist; **Black Tide** is the antagonist.

See `AI_STUDIO.md` for the static web upload flow used with Google AI Studio.

## Distribution

- `ANDROID_PLAY_STORE.md` — Capacitor / Play Store delivery.
- `GOOGLE_PLAY_LISTING_IT.md` — Italian Play Store listing (kept for reference).
- `DISTRIBUTION.md` — channels and rollouts.
- `play-store/` — feature graphic and screenshots.

## Production memory

See `MEMORY.md` for the running log of design decisions and verified constraints.