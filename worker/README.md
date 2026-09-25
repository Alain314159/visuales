# visuales-proxy

Cloudflare Worker que actua como proxy hacia Visuales UCLV y anade CORS.

## Endpoints

- GET /               -> health check
- GET /api/catalog    -> lista un directorio (JSON)
- GET /api/file       -> proxy con soporte de Range

## Deploy

    cd worker
    npm install
    npx wrangler login
    npm run deploy

Wrangler imprime la URL publica: https://visuales-proxy.TU-USUARIO.workers.dev
Copiala en Ajustes de la PWA.
