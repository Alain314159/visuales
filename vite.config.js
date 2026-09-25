import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { VitePWA } from 'vite-plugin-pwa';

// ⚠️ Si el repo en GitHub no se llama "visuales", cambia esta base.
export default defineConfig({
  base: '/visuales/',
  plugins: [
    vue(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg', 'icons/*.svg'],
      manifest: false,
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,png,ico,woff2}'],
        maximumFileSizeToCacheInBytes: 5 * 1024 * 1024,
        navigateFallback: '/visuales/index.html',
        runtimeCaching: [
          {
            // Nunca cachear las descargas grandes del worker
            urlPattern: ({ url }) => url.pathname.startsWith('/api/file'),
            handler: 'NetworkOnly'
          }
        ]
      }
    })
  ]
});
