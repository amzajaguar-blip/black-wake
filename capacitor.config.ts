import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "com.blackwake.game",
  appName: "Black Wake",
  webDir: "dist/public",
  bundledWebRuntime: false,
  android: {
    backgroundColor: "#041018",
  },
};

export default config;
