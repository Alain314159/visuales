// @visuales-downloader-v3
import { CapacitorDownloader } from '@capgo/capacitor-downloader';

let listenersReady = false;
let progressHandler = null;
let completedHandler = null;
let failedHandler = null;

export function initDownloader({ onProgress, onCompleted, onFailed }) {
  progressHandler = onProgress;
  completedHandler = onCompleted;
  failedHandler = onFailed;

  if (listenersReady) return;
  listenersReady = true;

  CapacitorDownloader.addListener('downloadProgress', (event) => {
    if (progressHandler) progressHandler(event);
  });
  CapacitorDownloader.addListener('downloadCompleted', (event) => {
    if (completedHandler) completedHandler(event);
  });
  CapacitorDownloader.addListener('downloadFailed', (event) => {
    if (failedHandler) failedHandler(event);
  });
}

export async function startDownload({ id, url, filename, connections = 8 }) {
  return await CapacitorDownloader.download({
    id,
    url,
    fileName: filename,
    location: 'Visuales',
    connections
  });
}

export async function pauseDownload(id) {
  try { return await CapacitorDownloader.pause({ id }); }
  catch (e) { console.warn('pause:', e); }
}

export async function resumeDownload(id) {
  try { return await CapacitorDownloader.resume({ id }); }
  catch (e) { console.warn('resume:', e); }
}

export async function cancelDownload(id) {
  try { return await CapacitorDownloader.stop({ id }); }
  catch (e) { console.warn('stop:', e); }
}

export async function checkStatus(id) {
  try { return await CapacitorDownloader.checkStatus({ id }); }
  catch { return null; }
}

export async function getFileInfo(id) {
  try { return await CapacitorDownloader.getFileInfo({ id }); }
  catch { return null; }
}

export async function listNativeDownloads() {
  try { return await CapacitorDownloader.getList(); }
  catch { return { files: [] }; }
}
