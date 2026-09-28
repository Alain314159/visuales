// @visuales-storage-v1
import { Device } from '@capacitor/device';
import { Filesystem, Directory } from '@capacitor/filesystem';
import { Capacitor } from '@capacitor/core';

const IS_NATIVE = Capacitor.isNativePlatform();

export async function getDiskInfo() {
  try {
    const info = await Device.getInfo();
    return {
      free: info.realDiskFree || info.diskFree || 0,
      total: info.realDiskTotal || info.diskTotal || 0
    };
  } catch {
    return { free: 0, total: 0 };
  }
}

export function formatBytes(b) {
  if (!b) return '—';
  if (b >= 1073741824) return (b / 1073741824).toFixed(2) + ' GB';
  if (b >= 1048576) return (b / 1048576).toFixed(1) + ' MB';
  if (b >= 1024) return (b / 1024).toFixed(0) + ' KB';
  return b + ' B';
}

// Borra el archivo del celu. En Capacitor Downloader guarda en Directory.Documents/Visuales
export async function deleteDownloadedFile(filename) {
  if (!IS_NATIVE) return { ok: false, error: 'no nativo' };
  const paths = [
    { dir: Directory.Documents, path: 'Visuales/' + filename },
    { dir: Directory.External, path: 'Visuales/' + filename },
    { dir: Directory.ExternalStorage, path: 'Download/Visuales/' + filename }
  ];
  for (const p of paths) {
    try {
      await Filesystem.deleteFile(p);
      return { ok: true, path: p.path };
    } catch (e) {
      // seguimos probando
    }
  }
  return { ok: false, error: 'archivo no encontrado' };
}

export async function listDownloadedFiles() {
  if (!IS_NATIVE) return [];
  try {
    const res = await Filesystem.readdir({
      path: 'Visuales',
      directory: Directory.Documents
    });
    return res.files || [];
  } catch {
    return [];
  }
}
