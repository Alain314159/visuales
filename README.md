# Visuales

PWA para descarga acelerada de peliculas y series desde Visuales UCLV.

- Frontend: Vue 3 + Vite + Pinia + Vue Router + Dexie
- Proxy: Cloudflare Worker
- Descarga multi-hilo con pausa/reanudacion
- Guarda en carpeta elegida por el usuario (File System Access API)

## Desarrollo

    npm install
    npm run dev

## Deploy

Push a main -> GitHub Actions construye y despliega en GitHub Pages.

El Worker se despliega aparte:

    cd worker
    npm install
    npx wrangler login
    npm run deploy
