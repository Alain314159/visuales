// @visuales-poster-v1
import { Capacitor } from '@capacitor/core';
import { CapacitorHttp } from '@capacitor/core';

const cache = new Map();
const IS_NATIVE = Capacitor.isNativePlatform();

export async function loadPoster(url) {
  if (cache.has(url)) return cache.get(url);
  const promise = (async () => {
    try {
      if (IS_NATIVE) {
        const res = await CapacitorHttp.get({
          url,
          responseType: 'blob'
        });
        if (res.status === 200 && res.data) {
          return 'data:image/jpeg;base64,' + res.data;
        }
      } else {
        const res = await fetch(url);
        if (res.ok) {
          const blob = await res.blob();
          return URL.createObjectURL(blob);
        }
      }
    } catch (e) {
      console.warn('poster:', url, e);
    }
    return null;
  })();
  cache.set(url, promise);
  return promise;
}

export function clearPosterCache() {
  cache.clear();
}
