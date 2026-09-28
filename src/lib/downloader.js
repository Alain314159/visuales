// @visuales-downloader-v1
import { CapacitorDownloader } from '@capgo/capacitor-downloader';
import { Directory } from '@capacitor/filesystem';

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

export async function startDownload({ id, url, filename }) {
  return await CapacitorDownloader.download({
    id,
    url,
    location: 'Visuales',
    fileName: filename
  });
}

export async function pauseDownload(id) {
  try { return await CapacitorDownloader.pause({ id }); }
  catch (e) { console.warn('pause no soportado:', e); }
}

export async function resumeDownload(id) {
  try { return await CapacitorDownloader.resume({ id }); }
  catch (e) { console.warn('resume no soportado:', e); }
}

export async function cancelDownload(id) {
  try { return await CapacitorDownloader.stop({ id }); }
  catch (e) { console.warn('stop no soportado:', e); }
}

export async function listNativeDownloads() {
  try { return await CapacitorDownloader.getList(); }
  catch { return { files: [] }; }
}
