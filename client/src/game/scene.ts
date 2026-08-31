// Design reminder — The scene is a dark Mediterranean route: player's wake below, danger from the horizon.
import { Engine } from "@babylonjs/core/Engines/engine";
import { FreeCamera } from "@babylonjs/core/Cameras/freeCamera";
import { HemisphericLight } from "@babylonjs/core/Lights/hemisphericLight";
import { DirectionalLight } from "@babylonjs/core/Lights/directionalLight";
import { Color3 } from "@babylonjs/core/Maths/math.color";
import { Vector3 } from "@babylonjs/core/Maths/math.vector";
import { Scene } from "@babylonjs/core/scene";
import { GameWorld } from "./GameWorld";

export type GameHandle = { scene: Scene; dispose: () => void };

export async function createGameScene(engine: Engine, canvas: HTMLCanvasElement): Promise<GameHandle> {
  const scene = new Scene(engine);
  const camera = new FreeCamera("route-camera", new Vector3(0, 10.5, -18), scene);
  camera.setTarget(new Vector3(0, .2, 13));
  camera.fov = 0.94;
  camera.minZ = 0.1;
  camera.maxZ = 300;
  camera.attachControl(canvas, false);
  camera.inputs.clear();

  const skyLight = new HemisphericLight("moonlight", new Vector3(0, 1, 0), scene);
  skyLight.diffuse = Color3.FromHexString("#6abfc6");
  skyLight.groundColor = Color3.FromHexString("#061119");
  skyLight.intensity = 0.86;
  const rimLight = new DirectionalLight("portlight", new Vector3(-0.25, -1, 0.2), scene);
  rimLight.diffuse = Color3.FromHexString("#ff8c41");
  rimLight.intensity = 0.36;

  const world = new GameWorld(scene);
  scene.onBeforeRenderObservable.add(() => {
    const delta = scene.getEngine().getDeltaTime() / 1000;
    world.update(delta);
  });

  return { scene, dispose: () => { world.dispose(); scene.dispose(); } };
}
