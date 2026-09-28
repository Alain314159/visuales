import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { VitePWA } from 'vite-plugin-pwa';

const isCapacitor = process.env.CAP_BUILD === '1';

export default defineConfig({
  base: isCapacitor ? './' : '/visuales/',
  plugins: [
    vue(),
    !isCapacitor && VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg', 'icons/*.svg'],
      manifest: false,
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,png,ico,woff2}'],
        maximumFileSizeToCacheInBytes: 10 * 1024 * 1024,
        navigateFallback: '/visuales/index.html'
      }
    })
  ].filter(Boolean)
});
