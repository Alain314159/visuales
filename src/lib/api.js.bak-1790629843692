import { useAppStore } from '../stores/app';

function base() {
  const store = useAppStore();
  if (!store.workerUrl) throw new Error('Configura primero la URL del Worker en Ajustes');
  return store.workerUrl;
}

export async function health() {
  const res = await fetch(base() + '/');
  if (!res.ok) throw new Error('HTTP ' + res.status);
  return res.json();
}

export async function listDirectory(path) {
  const res = await fetch(base() + '/api/catalog?path=' + encodeURIComponent(path));
  if (!res.ok) throw new Error('HTTP ' + res.status);
  return res.json();
}
