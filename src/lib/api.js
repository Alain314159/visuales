// @visuales-capacitor-api-v1
import { parseApacheListing } from './scraper.js';

const VISUALES_BASE = 'https://visuales.uclv.cu';
const LS_KEY = 'visuales.baseUrl';

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

export function getBaseUrl() {
  return getBase();
}

export async function health() {
  try {
    const res = await fetch(getBase() + '/');
    return { ok: res.ok, status: res.status };
  } catch (e) {
    return { ok: false, error: String((e && e.message) || e) };
  }
}

export async function listDirectory(path) {
  const res = await fetch(getBase() + path);
  if (!res.ok) throw new Error('HTTP ' + res.status + ' al listar ' + path);
  const html = await res.text();
  const entries = parseApacheListing(html, path);
  return { path, count: entries.length, entries };
}

export function getDirectUrl(path) {
  return getBase() + path;
}
