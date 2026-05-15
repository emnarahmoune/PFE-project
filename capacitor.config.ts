import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.portailrh.app',
  appName: 'PortailRH',
  webDir: 'dist/pfe/browser',
  server: {
    cleartext: true
  }
};

export default config;