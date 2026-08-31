// Design reminder — Input is semantic and immediate: steer, burn fuel, or go quiet.
export type InputIntent = {
  laneDelta: number;
  boost: boolean;
  silent: boolean;
  pause: boolean;
  mute: boolean;
};

export class InputManager {
  private laneDelta = 0;
  private boost = false;
  private silent = false;
  private pause = false;
  private mute = false;
  private lastLaneAt = 0;
  private readonly keyDown: (event: KeyboardEvent) => void;
  private readonly keyUp: (event: KeyboardEvent) => void;

  constructor() {
    this.keyDown = (event) => {
      const key = event.key.toLowerCase();
      if (["arrowleft", "arrowright", "arrowup", "arrowdown", " "].includes(key)) event.preventDefault();
      if ((key === "a" || key === "arrowleft") && !event.repeat) this.queueLane(-1);
      if ((key === "d" || key === "arrowright") && !event.repeat) this.queueLane(1);
      if (key === "w" || key === "arrowup" || key === " ") this.boost = true;
      if (key === "s" || key === "arrowdown") this.silent = true;
      if ((key === "p" || key === "escape") && !event.repeat) this.pause = true;
      if (key === "m" && !event.repeat) this.mute = true;
    };
    this.keyUp = (event) => {
      const key = event.key.toLowerCase();
      if (key === "w" || key === "arrowup" || key === " ") this.boost = false;
      if (key === "s" || key === "arrowdown") this.silent = false;
    };
    window.addEventListener("keydown", this.keyDown, { passive: false });
    window.addEventListener("keyup", this.keyUp);
  }

  queueLane(delta: -1 | 1) {
    const now = performance.now();
    if (now - this.lastLaneAt < 95) return;
    this.lastLaneAt = now;
    this.laneDelta = delta;
  }

  setHeld(action: "boost" | "silent", held: boolean) {
    if (action === "boost") this.boost = held;
    if (action === "silent") this.silent = held;
  }

  consume(): InputIntent {
    const intent = { laneDelta: this.laneDelta, boost: this.boost, silent: this.silent, pause: this.pause, mute: this.mute };
    this.laneDelta = 0;
    this.pause = false;
    this.mute = false;
    return intent;
  }

  dispose() {
    window.removeEventListener("keydown", this.keyDown);
    window.removeEventListener("keyup", this.keyUp);
  }
}
