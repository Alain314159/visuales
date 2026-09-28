// @visuales-poster-v2
import { Capacitor } from '@capacitor/core';
import { CapacitorHttp } from '@capacitor/core';

const MAX_CACHE = 50;
const cache = new Map();
const IS_NATIVE = Capacitor.isNativePlatform();

function lruSet(key, value) {
  if (cache.size >= MAX_CACHE) {
    const firstKey = cache.keys().next().value;
    cache.delete(firstKey);
  }
  cache.set(key, value);
}

export async function loadPoster(url) {
  if (cache.has(url)) {
    const v = cache.get(url);
    cache.delete(url);
    cache.set(url, v);
    return v;
  }
  const promise = (async () => {
    try {
      if (IS_NATIVE) {
        const res = await CapacitorHttp.get({ url, responseType: 'blob' });
        if (res.status === 200 && res.data) return 'data:image/jpeg;base64,' + res.data;
      } else {
        const res = await fetch(url);
        if (res.ok) {
          const blob = await res.blob();
          return URL.createObjectURL(blob);
        }
      }
    } catch (e) { console.warn('poster:', url, e); }
    return null;
  })();
  lruSet(url, promise);
  return promise;
}

export function clearPosterCache() {
  cache.clear();
}
