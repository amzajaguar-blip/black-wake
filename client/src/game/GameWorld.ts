// Design reminder — A quiet sea is a decision surface; sectors make consequence visible before the collision.
import { Color3 } from "@babylonjs/core/Maths/math.color";
import { Color4 } from "@babylonjs/core/Maths/math.color";
import { Vector3 } from "@babylonjs/core/Maths/math.vector";
import { Mesh } from "@babylonjs/core/Meshes/mesh";
import { MeshBuilder } from "@babylonjs/core/Meshes/meshBuilder";
import { StandardMaterial } from "@babylonjs/core/Materials/standardMaterial";
import { Texture } from "@babylonjs/core/Materials/Textures/texture";
import { Scene } from "@babylonjs/core/scene";
import { CHAPTERS, DEFAULT_MODULES, MODULES, type Chapter, type Modules, type Sector } from "./data";
import { AudioDirector } from "./AudioDirector";
import { HudController } from "./HudController";
import { InputManager } from "./InputManager";

type EntityType = "intel" | "fuel" | "wreck" | "mine" | "enemy" | "fork";
type Entity = { type: EntityType; lane: number; mesh: Mesh; z: number; value: number; label?: string; resolved?: boolean };
type Mode = "menu" | "briefing" | "running" | "paused" | "garage" | "debrief";

const LANES = [-7, 0, 7];
const STORAGE_KEY = "marea-nera-operational-save-v3";

export class GameWorld {
  private readonly entities: Entity[] = [];
  private readonly waterMarks: Mesh[] = [];
  private readonly input = new InputManager();
  private readonly hud = new HudController();
  private readonly audio = new AudioDirector();
  private player: Mesh;
  private playerGlow: Mesh;
  private mode: Mode = "menu";
  private sectorIndex = 0;
  private currentChapterIndex = 0;
  private unlockedChapterCount = 1;
  private sectorElapsed = 0;
  private runElapsed = 0;
  private spawnClock = 0;
  private lane = 1;
  private hull = 100;
  private fuel = 100;
  private intel = 0;
  private detection = 0.12;
  private peakDetection = 0.12;
  private invulnerable = 0;
  private drift = 0;
  private pursuitCooldown = 0;
  private forkSpawned = false;
  private forkNotice: string | null = null;
  private forkExpire = 0;
  private muted = false;
  private modules: Modules = { ...DEFAULT_MODULES };
  private intelBank = 0;
  private bestIntel = 0;
  private demo = new URLSearchParams(window.location.search).has("demo");
  private debugChapter = Number(new URLSearchParams(window.location.search).get("chapter"));
  private debugAlert = new URLSearchParams(window.location.search).has("alert");
  private randomSeed = 17411;

  constructor(private scene: Scene) {
    this.scene.clearColor = new Color4(0.011, 0.041, 0.062, 1);
    this.player = this.createPlayer();
    this.playerGlow = this.createWake(this.player.position.z - 2.4, 0.16, 1.25);
    this.createSea();
    this.loadSave();
    if (Number.isInteger(this.debugChapter) && this.debugChapter >= 1 && this.debugChapter <= CHAPTERS.length) {
      this.currentChapterIndex = this.debugChapter - 1;
      this.unlockedChapterCount = CHAPTERS.length;
    }
    this.hud.setTouchHandlers({
      left: () => { this.audio.unlock(); this.input.queueLane(-1); },
      right: () => { this.audio.unlock(); this.input.queueLane(1); },
      boost: (held) => { this.audio.unlock(); this.input.setHeld("boost", held); },
      silent: (held) => { this.audio.unlock(); this.input.setHeld("silent", held); },
      pause: () => { this.audio.unlock(); this.togglePause(); },
    });
    if (this.demo) window.setTimeout(() => this.startRun(), 140);
    else this.openMenu();
  }

  update(delta: number) {
    this.updateSea(delta);
    const intent = this.input.consume();
    if (intent.pause) { this.togglePause(); return; }
    if (this.mode !== "running") return;
    const dt = Math.min(delta, 0.05) * (this.demo ? 1.42 : 1);
    if (intent.mute) { this.muted = !this.muted; this.audio.setMuted(this.muted); this.hud.flash(this.muted ? "COMMS DISATTIVATE" : "COMMS ATTIVATE", "amber"); }

    this.stepAutopilot();
    this.stepPlayer(dt, intent);
    this.audio.tick({ boost: intent.boost, silent: intent.silent, detection: this.detection, active: true, extract: this.sector.extract });
    this.stepSector(dt);
    if (this.mode !== "running") return;
    this.stepEntities(dt);
    this.updateHud(intent.boost, intent.silent);
  }

  private createSea() {
    const sea = MeshBuilder.CreateGround("sea", { width: 36, height: 170, subdivisions: 2 }, this.scene);
    sea.position = new Vector3(0, -0.42, 58);
    const seaMaterial = new StandardMaterial("sea-material", this.scene);
    seaMaterial.diffuseColor = Color3.FromHexString("#061d2a");
    seaMaterial.specularColor = Color3.FromHexString("#1d6572");
    seaMaterial.specularPower = 68;
    sea.material = seaMaterial;

    this.createPortSilhouette();

    for (let i = 0; i < 32; i += 1) {
      const lane = i % 4 === 0 ? 0 : (i % 3) - 1;
      const z = 2 + i * 4.1;
      const mark = this.createWake(z, 0.025, 0.22 + (i % 5) * 0.12, lane * 5.2 + ((i % 2) * 1.2 - 0.6));
      mark.name = `water-mark-${i}`;
      this.waterMarks.push(mark);
    }

    [-3.5, 3.5].forEach((side) => {
      const route = MeshBuilder.CreateLines(`route-${side}`, { points: [new Vector3(side, -0.27, -7), new Vector3(side * 2.35, -0.27, 122)] }, this.scene);
      route.color = Color3.FromHexString("#176a71");
      route.alpha = 0.68;
    });
    [-12, 12].forEach((side) => {
      const boundary = MeshBuilder.CreateLines(`boundary-${side}`, { points: [new Vector3(side, -0.26, -7), new Vector3(side * 1.5, -0.26, 122)] }, this.scene);
      boundary.color = Color3.FromHexString("#1d4650");
      boundary.alpha = 0.55;
    });
    [18, 42, 67, 94].forEach((z, index) => {
      const arc = MeshBuilder.CreateTorus(`sonar-arc-${index}`, { thickness: .055, diameter: 7 + index * 1.7, tessellation: 28 }, this.scene);
      arc.position = new Vector3(0, -.22, z);
      arc.rotation.x = Math.PI / 2;
      const arcMaterial = new StandardMaterial(`sonar-arc-mat-${index}`, this.scene);
      arcMaterial.emissiveColor = Color3.FromHexString(index % 2 ? "#0f686d" : "#1aa8a6");
      arcMaterial.alpha = .35;
      arcMaterial.disableLighting = true;
      arc.material = arcMaterial;
    });
  }

  private createPlayer() {
    const hull = this.createBoat("elias-boat", "#132f38", "#0b141a", "#2be7e0");
    hull.position = new Vector3(0, 0.18, 1.8);
    return hull;
  }

  private createBoat(name: string, hullColor: string, deckColor: string, accent: string) {
    const hull = MeshBuilder.CreateBox(name, { width: 2.85, height: 0.7, depth: 5.1 }, this.scene);
    const hullMaterial = new StandardMaterial(`${name}-hull`, this.scene);
    hullMaterial.diffuseColor = Color3.FromHexString(hullColor);
    hullMaterial.emissiveColor = Color3.FromHexString("#061016");
    hull.material = hullMaterial;
    const deck = MeshBuilder.CreateBox(`${name}-deck`, { width: 1.55, height: 0.43, depth: 2.45 }, this.scene);
    deck.parent = hull;
    deck.position = new Vector3(0, 0.48, -0.3);
    const deckMaterial = new StandardMaterial(`${name}-deck-mat`, this.scene);
    deckMaterial.diffuseColor = Color3.FromHexString(deckColor);
    deck.material = deckMaterial;
    const bowLight = MeshBuilder.CreateSphere(`${name}-light`, { diameter: 0.24, segments: 8 }, this.scene);
    bowLight.parent = hull;
    bowLight.position = new Vector3(0, 0.55, 2.18);
    const lightMaterial = new StandardMaterial(`${name}-light-mat`, this.scene);
    lightMaterial.emissiveColor = Color3.FromHexString(accent);
    lightMaterial.disableLighting = true;
    bowLight.material = lightMaterial;
    return hull;
  }

  private createPortSilhouette() {
    const skyline = new StandardMaterial("port-silhouette", this.scene);
    skyline.diffuseColor = Color3.FromHexString("#071822");
    skyline.emissiveColor = Color3.FromHexString("#0b2730");
    const beacon = new StandardMaterial("port-beacon", this.scene);
    beacon.emissiveColor = Color3.FromHexString("#ff8440");
    beacon.disableLighting = true;
    [-14, -9, -4, 5, 11, 16].forEach((x, index) => {
      const h = 4 + (index % 3) * 2.1;
      const building = MeshBuilder.CreateBox(`port-stack-${index}`, { width: 3.6, height: h, depth: 3.8 }, this.scene);
      building.position = new Vector3(x, h / 2 - 0.32, 74 + (index % 2) * 5);
      building.material = skyline;
      const light = MeshBuilder.CreateSphere(`port-light-${index}`, { diameter: .42, segments: 8 }, this.scene);
      light.position = new Vector3(x + 1, h + .15, 74 + (index % 2) * 5);
      light.material = beacon;
    });
    [-10, 9].forEach((x, index) => {
      const crane = MeshBuilder.CreateBox(`crane-${index}`, { width: .5, height: 13, depth: .55 }, this.scene);
      crane.position = new Vector3(x, 6, 94);
      crane.material = skyline;
      const arm = MeshBuilder.CreateBox(`crane-arm-${index}`, { width: 9, height: .42, depth: .42 }, this.scene);
      arm.position = new Vector3(x + (index ? -3 : 3), 11.8, 94);
      arm.material = skyline;
    });
  }

  private createWake(z: number, alpha: number, scale: number, x = 0) {
    const wake = MeshBuilder.CreateBox("wake", { width: 1.15 * scale, height: 0.02, depth: 5.8 * scale }, this.scene);
    wake.position = new Vector3(x, -0.18, z);
    const material = new StandardMaterial(`wake-mat-${Math.random()}`, this.scene);
    material.emissiveColor = Color3.FromHexString("#21c9c5");
    material.alpha = alpha;
    material.disableLighting = true;
    wake.material = material;
    return wake;
  }

  private updateSea(delta: number) {
    this.waterMarks.forEach((mark, index) => {
      mark.position.z -= delta * (12 + (index % 4) * 2.2);
      if (mark.position.z < -10) mark.position.z += 134;
    });
    const pulse = 0.1 + Math.sin(performance.now() / 165) * 0.04;
    (this.playerGlow.material as StandardMaterial).alpha = pulse;
  }

  private stepPlayer(dt: number, intent: ReturnType<InputManager["consume"]>) {
    this.invulnerable = Math.max(0, this.invulnerable - dt);
    if (intent.laneDelta) { this.lane = Math.max(0, Math.min(2, this.lane + intent.laneDelta)); this.audio.cue("lane"); }
    const targetX = LANES[this.lane];
    this.player.position.x += (targetX - this.player.position.x) * Math.min(1, dt * 12);
    this.player.rotation.z = (targetX - this.player.position.x) * -0.055;
    this.playerGlow.position.x = this.player.position.x;

    const canBoost = this.fuel > 5 && this.drift <= 0;
    const boost = intent.boost && canBoost;
    const silent = intent.silent && !boost;
    if (boost) this.audio.cueState("boost");
    else if (silent) this.audio.cueState("silent");
    else this.audio.resetState();
    let movement = 1;
    if (boost) movement = 1.64 + this.modules.engine * 0.05;
    if (silent) movement = 0.55;
    if (this.fuel <= 0) { this.drift = Math.max(0, this.drift - dt); movement = 0.33; }

    this.fuel = Math.max(0, this.fuel - dt * (1.0 + (boost ? 5.6 : 0)));
    if (boost) this.detection = Math.min(1, this.detection + dt * 0.115);
    else if (silent) this.detection = Math.max(0.025, this.detection - dt * (0.16 + this.modules.stealth * 0.04));
    else this.detection = Math.max(0.04, this.detection - dt * 0.02);
    if (this.fuel === 0 && this.drift <= 0) {
      this.drift = 3;
      this.hud.flash("CARBURANTE ESAURITO // DERIVA", "red");
    }
    if (this.fuel === 0 && this.drift === 0) this.hit(2.3 * dt, "Il mare entra nello scafo.");
    this.player.position.z = 1.8 + (movement - 1) * 0.12;
    this.playerGlow.position.z = this.player.position.z - 2.45;
    this.peakDetection = Math.max(this.peakDetection, this.detection);

    if (this.detection > 0.91 && this.pursuitCooldown <= 0) {
      this.pursuitCooldown = 8;
      this.hud.flash("CONTATTO PIENO // ONDA D'INSEGUIMENTO", "red");
      this.audio.cue("threat");
      this.audio.radio("MAYA: Hanno agganciato la tua scia. Rompi la firma.", true);
      [0, 1, 2].forEach((lane) => this.spawnEntity("enemy", lane, 66 + lane * 4));
    }
    this.pursuitCooldown -= dt;
  }

  private stepSector(dt: number) {
    const sector = this.sector;
    this.runElapsed += dt;
    this.sectorElapsed += dt;
    this.spawnClock -= dt;
    if (this.spawnClock <= 0) {
      this.spawnForSector(sector);
      this.spawnClock = Math.max(0.55, 1.25 - sector.density * 0.48 + Math.random() * 0.35);
    }
    if (sector.fork && !this.forkSpawned && this.sectorElapsed > 3.2) this.spawnFork();
    if (this.sectorElapsed >= sector.duration) {
      if (sector.extract) {
        const won = this.intel >= Math.ceil(this.chapter.intelGoal * 0.7) && this.hull > 0;
        this.finish(won, won ? "I dati lasciano il mare. Per questa notte, abbastanza." : "La trasmissione è incompleta. Il segnale si spegne.");
        return;
      }
      this.sectorIndex += 1;
      this.sectorElapsed = 0;
      this.forkSpawned = false;
      this.applyEnvironment(this.sector.environment);
      this.hud.flash(`${this.sector.title} // ${this.sector.tag}`, "cyan");
      this.hud.flash(this.sector.radio, "amber");
      this.audio.radio(this.sector.radio);
    }
  }

  private spawnForSector(sector: Sector) {
    const roll = this.random();
    const threatMultiplier = this.detection > 0.65 ? 1.5 : 1;
    let type: EntityType;
    if (roll < sector.intelBias * 0.52) type = "intel";
    else if (roll < sector.intelBias * 0.52 + 0.12) type = "fuel";
    else if (roll < sector.intelBias * 0.52 + sector.threat * 0.32 * threatMultiplier) type = this.random() > 0.5 ? "enemy" : "mine";
    else type = "wreck";
    const lane = Math.floor(this.random() * 3);
    this.spawnEntity(type, lane, 96 + this.random() * 18);
  }

  private spawnFork() {
    this.forkSpawned = true;
    this.forkNotice = "FORCA IN ARRIVO // 01 SICURO · 02 INTEL · 03 VOLATILE";
    this.audio.cue("fork");
    this.forkExpire = this.runElapsed + 6.2;
    this.spawnEntity("fork", 0, 84, 0, "01 // SICURO");
    this.spawnEntity("fork", 1, 84, 1, "02 // INTEL");
    this.spawnEntity("fork", 2, 84, 2, "03 // VOLATILE");
  }

  private spawnEntity(type: EntityType, lane: number, z: number, value = 0, label?: string) {
    let mesh: Mesh;
    if (type === "intel") {
      mesh = MeshBuilder.CreateBox("intel-case", { width: 1.42, height: .74, depth: 1.68 }, this.scene);
      mesh.position.y = 0.25;
      const material = new StandardMaterial("intel-material", this.scene);
      material.diffuseColor = Color3.FromHexString("#071e25");
      material.emissiveColor = Color3.FromHexString("#18aaa8");
      mesh.material = material;
    } else if (type === "fuel") {
      mesh = MeshBuilder.CreateBox("fuel-can", { width: 1.25, height: 1.32, depth: 1.02 }, this.scene);
      mesh.position.y = 0.42;
      const material = new StandardMaterial("fuel-material", this.scene);
      material.diffuseColor = Color3.FromHexString("#e46d26");
      material.emissiveColor = Color3.FromHexString("#8a2608");
      mesh.material = material;
    } else if (type === "mine") {
      mesh = MeshBuilder.CreateSphere("mine", { diameter: 1.65, segments: 10 }, this.scene);
      mesh.position.y = 0.42;
      const material = new StandardMaterial("mine-material", this.scene);
      material.diffuseColor = Color3.FromHexString("#151a1e");
      material.emissiveColor = Color3.FromHexString("#7b161d");
      mesh.material = material;
    } else if (type === "enemy") {
      mesh = this.createBoat("black-tide", "#14151b", "#252c34", "#f04b4f");
    } else if (type === "fork") {
      mesh = MeshBuilder.CreateTorus("route-gate", { thickness: 0.2, diameter: 3.3, tessellation: 20 }, this.scene);
      mesh.rotation.x = Math.PI / 2;
      const colors = ["#2BE7E0", "#ffb347", "#e64649"];
      const material = new StandardMaterial("route-gate-material", this.scene);
      material.emissiveColor = Color3.FromHexString(colors[lane]);
      material.disableLighting = true;
      mesh.material = material;
    } else {
      mesh = MeshBuilder.CreateBox("wreck", { width: 4.5, height: 1.5, depth: 5.9 }, this.scene);
      mesh.position.y = 0.35;
      mesh.rotation.y = (lane - 1) * 0.24;
      const material = new StandardMaterial("wreck-material", this.scene);
      material.diffuseColor = Color3.FromHexString("#35444a");
      material.emissiveColor = Color3.FromHexString("#18262a");
      mesh.material = material;
    }
    mesh.position.x = LANES[lane];
    mesh.position.z = z;
    this.entities.push({ type, lane, mesh, z, value, label });
  }

  private stepEntities(dt: number) {
    const speed = 18 + this.sector.threat * 4 + (this.detection > 0.65 ? 3.5 : 0);
    for (let index = this.entities.length - 1; index >= 0; index -= 1) {
      const entity = this.entities[index];
      entity.z -= dt * speed;
      entity.mesh.position.z = entity.z;
      if (entity.type === "intel" || entity.type === "fuel" || entity.type === "mine") entity.mesh.rotation.y += dt * 1.4;
      if (entity.type === "fork") entity.mesh.rotation.z += dt * 0.55;

      if (!entity.resolved && entity.z < 0.7 && entity.z > -6.4 && entity.lane === this.lane) {
        entity.resolved = true;
        this.resolveEntity(entity);
      }
      if (entity.z < -12 || entity.resolved) {
        entity.mesh.dispose();
        this.entities.splice(index, 1);
      }
    }
    if (this.forkNotice && this.runElapsed > this.forkExpire) this.forkNotice = null;
  }

  private resolveEntity(entity: Entity) {
    switch (entity.type) {
      case "intel":
        this.intel += 1;
        this.hud.flash("INTEL RECUPERATA +1", "cyan");
        this.audio.cue("intel");
        break;
      case "fuel":
        this.fuel = Math.min(100, this.fuel + 20 + this.modules.tank * 4);
        this.hud.flash("CARBURANTE RECUPERATO", "amber");
        this.audio.cue("fuel");
        break;
      case "wreck":
        this.hit(20 - this.modules.hull * 3, "Relitto colpito. Lo scafo grida.");
        break;
      case "mine":
        this.hit(25 - this.modules.hull * 3, "Mina. L'acqua entra.");
        break;
      case "enemy":
        this.detection = Math.min(1, this.detection + 0.18);
        this.hit(14 - this.modules.hull * 2, "BLACK TIDE è troppo vicina.");
        break;
      case "fork":
        this.resolveFork(entity);
        break;
    }
  }

  private resolveFork(entity: Entity) {
    this.entities.filter((candidate) => candidate.type === "fork").forEach((candidate) => { candidate.resolved = true; });
    this.forkNotice = null;
    if (entity.value === 0) {
      this.detection = Math.max(0.04, this.detection - 0.16);
      this.hud.flash("ROTTA SICURA // SEGNATURA RIDOTTA", "cyan");
      this.audio.cue("silent");
    } else if (entity.value === 1) {
      this.intel += 3;
      this.detection = Math.min(1, this.detection + 0.1);
      this.hud.flash("ROUTE INTEL // +3 DATI, CONTATTO AUMENTA", "amber");
      this.audio.cue("intel");
    } else {
      this.fuel = Math.min(100, this.fuel + 28);
      this.detection = Math.min(1, this.detection + 0.26);
      this.spawnEntity("enemy", (this.lane + 1) % 3, 48);
      this.hud.flash("ROUTE VOLATILE // CARBURANTE, MA TI HANNO VISTO", "red");
      this.audio.cue("threat");
    }
  }

  private hit(amount: number, reason: string) {
    if (this.invulnerable > 0) return;
    this.invulnerable = 0.86;
    this.hull = Math.max(0, this.hull - amount);
    this.hud.flash(`IMPATTO // ${reason}`, "red");
    this.audio.cue("impact");
    if (this.hull <= 0) this.finish(false, "Lo scafo non regge. La prova resta sotto la marea.");
  }

  private updateHud(boost: boolean, silent: boolean) {
    this.hud.render({
      hull: this.hull,
      fuel: this.fuel,
      intel: this.intel,
      intelGoal: this.chapter.intelGoal,
      chapterCode: this.chapter.code,
      detection: this.detection,
      sector: this.sector,
      sectorElapsed: this.sectorElapsed,
      paused: this.mode === "paused",
      muted: this.muted,
      boost,
      silent,
      forkNotice: this.forkNotice,
      transmission: this.sector.extract ? (this.sectorElapsed / this.sector.duration) * 100 : null,
      drift: this.drift,
    });
  }

  private get chapter(): Chapter { return CHAPTERS[this.currentChapterIndex]; }
  private get sector() { return this.chapter.sectors[Math.min(this.sectorIndex, this.chapter.sectors.length - 1)]; }

  private random() {
    if (!this.demo) return Math.random();
    this.randomSeed = (this.randomSeed * 16807) % 2147483647;
    return (this.randomSeed - 1) / 2147483646;
  }

  private stepAutopilot() {
    if (!this.demo) return;
    const pattern = [1, 0, 2, 1, 2, 0, 1];
    const target = pattern[Math.floor(this.runElapsed / 2.4) % pattern.length];
    this.lane = target;
  }

  private openMenu() {
    this.mode = "menu";
    this.cleanupRun();
    this.audio.tick({ boost: false, silent: false, detection: 0, active: false });
    this.hud.showMenu(CHAPTERS, this.unlockedChapterCount, (index) => { this.currentChapterIndex = index; this.openBriefing(); }, () => this.openGarage(), this.intelBank, this.bestIntel);
  }

  private openBriefing() { this.mode = "briefing"; this.audio.tick({ boost: false, silent: false, detection: 0, active: false }); this.hud.showBriefing(this.chapter, () => this.startRun(), () => this.openMenu()); }

  private startRun() {
    this.audio.unlock();
    this.audio.setMuted(this.muted);
    this.audio.cue("start");
    this.cleanupRun();
    this.mode = "running";
    this.hull = 100 + this.modules.hull * 7;
    this.fuel = 100;
    this.intel = 0;
    this.detection = this.debugAlert ? .9 : 0.12;
    this.peakDetection = 0.12;
    this.sectorIndex = 0;
    this.sectorElapsed = 0;
    this.runElapsed = 0;
    this.randomSeed = 17411;
    this.spawnClock = 0.42;
    this.lane = 1;
    this.player.position.x = 0;
    this.applyEnvironment(this.sector.environment);
    this.hud.showGameplay();
    this.hud.flash(`CAP. ${this.chapter.code} // ${this.sector.title}`, "cyan");
    window.setTimeout(() => { this.hud.flash(this.chapter.maya, "amber"); this.audio.radio(`MAYA: ${this.chapter.maya}`); }, 950);
  }

  private applyEnvironment(environment: Sector["environment"]) {
    const colors: Record<Sector["environment"], Color4> = {
      night: new Color4(.011, .041, .062, 1),
      port: new Color4(.025, .049, .055, 1),
      storm: new Color4(.023, .035, .071, 1),
      extract: new Color4(.012, .072, .073, 1),
      island: new Color4(.018, .058, .050, 1),
      dawn: new Color4(.062, .050, .055, 1),
    };
    this.scene.clearColor = colors[environment];
  }

  private togglePause() {
    if (this.mode === "running") {
      this.mode = "paused";
      this.audio.tick({ boost: false, silent: false, detection: this.detection, active: false });
      this.hud.showPause(() => { this.mode = "running"; this.hud.showGameplay(); }, () => this.openMenu());
    } else if (this.mode === "paused") {
      this.mode = "running";
      this.audio.tick({ boost: false, silent: false, detection: this.detection, active: true, extract: this.sector.extract });
      this.hud.showGameplay();
    }
  }

  private openGarage() {
    this.mode = "garage";
    this.audio.tick({ boost: false, silent: false, detection: 0, active: false });
    this.hud.showGarage(this.modules, this.intelBank, (id) => this.buyModule(id), () => this.openMenu());
  }

  private buyModule(id: keyof Modules) {
    const module = MODULES.find((item) => item.id === id);
    if (!module || this.modules[id] >= 3) return;
    const cost = module.costs[this.modules[id]];
    if (this.intelBank < cost) { this.hud.flash("INTEL INSUFFICIENTE", "red"); return; }
    this.intelBank -= cost;
    this.modules[id] += 1;
    this.save();
    this.hud.showGarage(this.modules, this.intelBank, (next) => this.buyModule(next), () => this.openMenu());
  }

  private finish(won: boolean, reason: string) {
    if (this.mode !== "running") return;
    this.mode = "debrief";
    const banked = won ? this.intel : Math.floor(this.intel * 0.4);
    const nextUnlocked = won && this.currentChapterIndex + 1 === this.unlockedChapterCount && this.currentChapterIndex < CHAPTERS.length - 1;
    if (won) this.unlockedChapterCount = Math.min(CHAPTERS.length, Math.max(this.unlockedChapterCount, this.currentChapterIndex + 2));
    this.intelBank += banked;
    this.bestIntel = Math.max(this.bestIntel, this.intel);
    this.save();
    this.audio.cue(won ? "extract" : "fail");
    this.audio.tick({ boost: false, silent: false, detection: this.detection, active: false });
    this.hud.showDebrief({ won, intel: this.intel, banked, reason, peakSignal: this.peakDetection, chapter: this.chapter, nextUnlocked }, () => {
      if (won && this.currentChapterIndex < CHAPTERS.length - 1) { this.currentChapterIndex += 1; this.openBriefing(); }
      else if (won) this.openMenu();
      else this.openBriefing();
    }, () => this.openBriefing(), () => this.openMenu());
  }

  private cleanupRun() {
    while (this.entities.length) this.entities.pop()?.mesh.dispose();
    this.forkNotice = null;
    this.drift = 0;
    this.invulnerable = 0;
    this.audio.resetState();
  }

  private loadSave() {
    try {
      const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || localStorage.getItem("marea-nera-operational-save-v2") || "{}");
      this.intelBank = Number(parsed.intelBank) || 0;
      this.bestIntel = Number(parsed.bestIntel) || 0;
      this.unlockedChapterCount = Math.min(CHAPTERS.length, Math.max(1, Number(parsed.unlockedChapterCount) || 1));
      this.modules = { ...DEFAULT_MODULES, ...(parsed.modules || {}) };
    } catch { this.modules = { ...DEFAULT_MODULES }; }
  }

  private save() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ intelBank: this.intelBank, bestIntel: this.bestIntel, unlockedChapterCount: this.unlockedChapterCount, modules: this.modules }));
  }

  dispose() {
    this.input.dispose();
    this.hud.dispose();
    this.audio.dispose();
    this.cleanupRun();
  }
}
