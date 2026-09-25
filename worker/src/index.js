// @visuales-worker-v2
import { parseApacheListing } from './scraper.js';

const UPSTREAM = 'https://visuales.uclv.cu';

const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, HEAD, OPTIONS',
  'Access-Control-Allow-Headers': 'Range, Content-Type, Accept',
  'Access-Control-Expose-Headers': 'Content-Length, Content-Range, Accept-Ranges'
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...CORS, 'Content-Type': 'application/json; charset=utf-8' }
  });
}

export default {
  async fetch(request) {
    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: CORS });
    }

    const url = new URL(request.url);

    try {
      if (url.pathname === '/' || url.pathname === '/health') {
        return json({ ok: true, service: 'visuales-proxy', version: '0.2.0' });
      }

      if (url.pathname === '/api/catalog') {
        return await handleCatalog(url);
      }

      if (url.pathname === '/api/file') {
        return await handleFile(request, url);
      }

      return new Response('Not Found', { status: 404, headers: CORS });
    } catch (e) {
      return json({ error: String(e && e.message || e) }, 500);
    }
  }
};

async function handleCatalog(url) {
  const path = url.searchParams.get('path') || '/';
  const upstreamUrl = UPSTREAM + path;

  const res = await fetch(upstreamUrl, {
    headers: { 'User-Agent': 'VisualesProxy/0.2 (+pwa)' }
  });

  if (!res.ok) {
    return json({ error: 'Upstream HTTP ' + res.status, path }, res.status);
  }

  const ct = res.headers.get('content-type') || '';
  if (!ct.includes('text/html')) {
    return json({ error: 'No es un directorio', path, contentType: ct }, 400);
  }

  const html = await res.text();
  const entries = parseApacheListing(html, path);

  return json({ path, count: entries.length, entries });
}

async function handleFile(request, url) {
  const target = url.searchParams.get('url');
  if (!target) return json({ error: 'Falta parametro ?url=' }, 400);

  if (!target.startsWith(UPSTREAM + '/')) {
    return json({ error: 'URL no permitida (solo visuales.uclv.cu)' }, 403);
  }

  const headers = new Headers();
  const range = request.headers.get('Range');
  if (range) headers.set('Range', range);
  headers.set('User-Agent', 'VisualesProxy/0.2 (+pwa)');

  const upstream = await fetch(target, {
    method: request.method === 'HEAD' ? 'HEAD' : 'GET',
    headers
  });

  const respHeaders = new Headers(CORS);
  for (const h of [
    'Content-Type', 'Content-Length', 'Content-Range',
    'Accept-Ranges', 'Last-Modified', 'ETag'
  ]) {
    const v = upstream.headers.get(h);
    if (v) respHeaders.set(h, v);
  }

  return new Response(upstream.body, {
    status: upstream.status,
    headers: respHeaders
  });
}
