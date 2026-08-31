import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import { storagePut } from "../server/storage.ts";

const root = resolve(import.meta.dirname, "..");
const tracks = ["route", "pursuit", "uplink"];
const results = {};

for (const track of tracks) {
  const filename = `marea-nera-${track}-bed.mp3`;
  const bytes = await readFile(`/home/ubuntu/webdev-static-assets/${filename}`);
  results[track] = await storagePut(`marea-nera/audio/${filename}`, bytes, "audio/mpeg");
}

console.log(JSON.stringify(results, null, 2));
