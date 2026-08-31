// Design reminder — The page is intentionally only the game frame; all visible composition belongs to the operational sea.
import GameCanvas from "./components/GameCanvas";
import AudioMixer from "./components/AudioMixer";

export default function App() {
  return <><GameCanvas /><AudioMixer /></>;
}
