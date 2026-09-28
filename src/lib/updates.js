// @visuales-updates-v1
import pkg from '../../package.json';
import { Capacitor } from '@capacitor/core';
import { CapacitorHttp } from '@capacitor/core';

const CURRENT = pkg.version || '0.0.0';
const REPO = 'Alain314159/visuales';
const IS_NATIVE = Capacitor.isNativePlatform();

export function currentVersion() {
  return CURRENT;
}

export async function checkForUpdates() {
  const url = 'https://api.github.com/repos/' + REPO + '/releases/latest';
  try {
    let data;
    if (IS_NATIVE) {
      const res = await CapacitorHttp.get({
        url,
        headers: { 'Accept': 'application/vnd.github+json' }
      });
      data = res.data;
    } else {
      const res = await fetch(url, { headers: { 'Accept': 'application/vnd.github+json' } });
      data = await res.json();
    }
    const latestTag = data.tag_name || '';
    const apkAsset = (data.assets || []).find(a => a.name.endsWith('.apk'));
    return {
      current: CURRENT,
      latest: latestTag,
      hasUpdate: latestTag !== 'v' + CURRENT && latestTag !== CURRENT,
      downloadUrl: apkAsset ? apkAsset.browser_download_url : data.html_url,
      releaseUrl: data.html_url,
      publishedAt: data.published_at
    };
  } catch (e) {
    return { error: String(e.message || e), current: CURRENT };
  }
}
