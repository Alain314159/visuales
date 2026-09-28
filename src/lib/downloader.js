// @visuales-downloader-v1
import { Downloader } from '@capgo/capacitor-downloader';
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

  Downloader.addListener('downloadProgress', (event) => {
    if (progressHandler) progressHandler(event);
  });
  Downloader.addListener('downloadCompleted', (event) => {
    if (completedHandler) completedHandler(event);
  });
  Downloader.addListener('downloadFailed', (event) => {
    if (failedHandler) failedHandler(event);
  });
}

export async function startDownload({ id, url, filename }) {
  return await Downloader.download({
    id,
    url,
    location: 'Visuales',
    fileName: filename
  });
}

export async function pauseDownload(id) {
  try { return await Downloader.pause({ id }); }
  catch (e) { console.warn('pause no soportado:', e); }
}

export async function resumeDownload(id) {
  try { return await Downloader.resume({ id }); }
  catch (e) { console.warn('resume no soportado:', e); }
}

export async function cancelDownload(id) {
  try { return await Downloader.stop({ id }); }
  catch (e) { console.warn('stop no soportado:', e); }
}

export async function listNativeDownloads() {
  try { return await Downloader.getList(); }
  catch { return { files: [] }; }
}
