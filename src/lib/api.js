// @visuales-api-v3
import { Capacitor } from '@capacitor/core';
import { CapacitorHttp } from '@capacitor/core';
import { parseApacheListing } from './scraper.js';

const VISUALES_BASE = 'https://visuales.uclv.cu';
const LS_KEY = 'visuales.baseUrl';
const IS_NATIVE = Capacitor.isNativePlatform();

function getBase() {
  try {
    return (localStorage.getItem(LS_KEY) || VISUALES_BASE).replace(/\/$/, '');
  } catch {
    return VISUALES_BASE;
  }
}

export function setBase(url) {
  localStorage.setItem(LS_KEY, (url || '').trim().replace(/\/$/, ''));
}

export function getBaseUrl() { return getBase(); }
export function isNative() { return IS_NATIVE; }
export function getDirectUrl(path) { return getBase() + path; }

async function fetchText(url) {
  if (IS_NATIVE) {
    const res = await CapacitorHttp.get({ url });
    if (res.status < 200 || res.status >= 300) {
      throw new Error('HTTP ' + res.status);
    }
    return res.data;
  }
  const res = await fetch(url);
  if (!res.ok) throw new Error('HTTP ' + res.status);
  return await res.text();
}

export async function health() {
  const url = getBase() + '/';
  try {
    if (IS_NATIVE) {
      const res = await CapacitorHttp.get({ url });
      return { ok: res.status >= 200 && res.status < 300, status: res.status };
    }
    const res = await fetch(url);
    return { ok: res.ok, status: res.status };
  } catch (e) {
    return { ok: false, error: String((e && e.message) || e) };
  }
}

export async function listDirectory(path) {
  const url = getBase() + path;
  const html = await fetchText(url);
  const entries = parseApacheListing(html, path);
  return { path, count: entries.length, entries };
}

export async function getFileSize(url) {
  if (IS_NATIVE) {
    const res = await CapacitorHttp.request({
      url,
      method: 'HEAD',
      responseType: 'text'
    });
    const len = res.headers && res.headers['content-length'];
    return len ? parseInt(len, 10) : 0;
  }
  const res = await fetch(url, { method: 'HEAD' });
  const len = res.headers.get('Content-Length');
  return len ? parseInt(len, 10) : 0;
}
