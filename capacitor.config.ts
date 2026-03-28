import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.ketotracker.app',
  appName: 'Keto Tracker',
  // webDir points to the repo root — index.html lives here with no build step
  webDir: '.',
  android: {
    backgroundColor: '#060d18',
  },
  plugins: {
    // Filesystem: no extra config needed for Directory.Data (internal storage).
    // WRITE_EXTERNAL_STORAGE (maxSdkVersion 28) in AndroidManifest.xml covers
    // the Downloads export path on Android 9 and below.
  },
};

export default config;
