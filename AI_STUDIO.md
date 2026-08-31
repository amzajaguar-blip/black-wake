# AI Studio — static web upload

This repo ships a prebuilt static web bundle that you can upload to Google AI Studio as a **Web app** artifact.

## What to upload

Upload the **contents** of `dist/public/` (not the folder itself).

```
dist/public/
├── index.html              ← entry point
├── manifest.webmanifest    ← PWA manifest
├── assets/                 ← bundled JS + CSS
└── __manus__/              ← Manus runtime debug collector
```

> ⚠️ The `__manus__/` folder contains the Manus debug-collector runtime. It is safe to keep — it no-ops when the host is not Manus. If AI Studio complains about the script tag, delete `dist/public/__manus__/` before uploading. The debug script is also referenced from `index.html`; remove the `<script src="/__manus__/debug-collector.js" defer></script>` line too in that case.

## Upload steps

1. **Build the bundle locally** (skip if you already have a fresh `dist/public/`):

   ```bash
   pnpm install
   pnpm build
   ```

2. **Verify the bundle:**

   ```bash
   ls dist/public/index.html
   ls dist/public/manifest.webmanifest
   ls dist/public/assets/
   ```

3. **In Google AI Studio** (https://aistudio.google.com/):
   - New app → **Build with Gemini** → **Web app** (Streamlit/Html).
   - Select **Upload files** and drag the **contents** of `dist/public/`.
   - Set the **entry file** to `index.html`.
   - Confirm the app name: **Black Wake — Operation 07.14N**.

4. **Test the deploy:**
   - Open the AI Studio preview. The canvas should mount, the briefing screen should render, and `?demo` should drive a deterministic playable demo.

## Query flags

| URL flag | Effect |
| --- | --- |
| `?demo` | Activates a deterministic demo loop (no input required) |
| `?mute` | Starts the game with audio muted |

## Rebuilding

The bundle is **gitignored** (see `.gitignore` → `dist/`). To refresh after code changes:

```bash
pnpm build         # rebuilds dist/public/
# then re-upload the new contents in AI Studio
```

## What AI Studio cannot do with this game

- **No multiplayer** — single-player only.
- **No Capacitor Android shell** — AI Studio serves the web bundle only. Android delivery goes through `pnpm sync:android` and `pnpm exec cap open android` (see `ANDROID_PLAY_STORE.md`).
- **No backend state** — the dev `server/_core` runs locally; AI Studio hosts static files only.

## Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| Blank canvas on preview | Bundle not uploaded, or upload missed `assets/` | Re-upload `dist/public/` contents |
| Audio does not start | Browser autoplay policy | Add a click-to-start overlay, or run `?mute` for silent demo |
| HUD is Italian and looks "wrong" in EN context | Code layer not translated by design | Expected — see README rebrand notes |