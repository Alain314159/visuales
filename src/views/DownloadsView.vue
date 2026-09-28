<script setup>
import { onMounted, computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useDownloadsStore } from '../stores/downloads';
import { getDirectUrl } from '../lib/api';
import { deleteDownloadedFile, formatBytes } from '../lib/storage';
import { CapacitorDownloader } from '@capgo/capacitor-downloader';
import {
  Download, Pause, Play, Trash2, Check, X, Loader2, FolderDown, ListX, Film, FileX
} from 'lucide-vue-next';

const route = useRoute();
const router = useRouter();
const store = useDownloadsStore();
const playing = ref(null);

onMounted(async () => {
  await store.load();
  if (route.query.url && route.query.name) {
    const path = String(route.query.url);
    const name = String(route.query.name);
    const size = parseInt(route.query.size || '0', 10);
    const subPath = String(route.query.sub || '');
    await store.enqueue({
      url: getDirectUrl(path), name, size,
      subtitleUrl: subPath ? getDirectUrl(subPath) : null
    });
    router.replace({ name: 'downloads' });
  }
});

async function play(item) {
  playing.value = item.id;
  try {
    const { FileOpener } = await import('@capawesome-team/capacitor-file-opener');
    const info = await CapacitorDownloader.getFileInfo({ id: item.id });
    if (info && info.path) {
      await FileOpener.open({ filePath: info.path, contentType: 'video/*' });
    }
  } catch (e) { console.warn('play:', e); }
  playing.value = null;
}

async function removeWithFile(item) {
  if (!confirm('¿Borrar también el archivo del dispositivo?')) {
    await store.remove(item.id);
    return;
  }
  const r = await deleteDownloadedFile(item.name);
  if (!r.ok) console.warn('no se pudo borrar archivo:', r.error);
  await store.remove(item.id);
}

const items = computed(() => store.items);
</script>

<template>
  <div>
    <div class="head">
      <h2>Descargas</h2>
      <button v-if="items.length" class="clear" @click="() => items.filter(i => i.status === 'completed' || i.status === 'failed').forEach(i => store.remove(i.id))">
        <ListX :size="16" /> Limpiar
      </button>
    </div>

    <div v-if="!items.length" class="empty">
      <FolderDown :size="48" />
      <p>Aún no hay descargas.</p>
      <p class="hint">Ve al catálogo y pulsa en una película o serie.</p>
    </div>

    <ul v-else class="list">
      <li v-for="d in items" :key="d.id" class="item">
        <div class="head-row">
          <Loader2 v-if="d.status === 'downloading'" :size="20" class="ico spin" />
          <Check v-else-if="d.status === 'completed'" :size="20" class="ico ok" />
          <X v-else-if="d.status === 'failed'" :size="20" class="ico err" />
          <Pause v-else-if="d.status === 'paused'" :size="20" class="ico paused" />
          <Download v-else :size="20" class="ico muted" />
          <span class="name">{{ d.name }}</span>
        </div>

        <div class="bar">
          <div class="fill" :class="{ ok: d.status === 'completed', err: d.status === 'failed' }" :style="{ width: (d.progress || 0) + '%' }"></div>
        </div>

        <div class="row">
          <span class="meta">
            {{ formatBytes(d.size) }} · {{ d.progress || 0 }}%
            <template v-if="d.retries"> · reintento {{ d.retries }}</template>
            <template v-if="d.error"> · {{ d.error }}</template>
          </span>
          <div class="actions">
            <button v-if="d.status === 'completed'" @click="play(d)" :disabled="playing === d.id" title="Reproducir">
              <Film :size="16" /> Reproducir
            </button>
            <button v-if="d.status === 'downloading'" @click="store.pause(d.id)" title="Pausar"><Pause :size="16" /></button>
            <button v-else-if="d.status === 'paused'" @click="store.resume(d.id)" title="Reanudar"><Play :size="16" /></button>
            <button v-if="d.status === 'completed'" @click="removeWithFile(d)" title="Borrar del celu"><FileX :size="16" /></button>
            <button v-else @click="store.remove(d.id)" title="Borrar de la lista"><Trash2 :size="16" /></button>
          </div>
        </div>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.clear { background: transparent; border: none; color: var(--muted); font-size: 13px; display: inline-flex; gap: 4px; align-items: center; }
.empty { display: flex; flex-direction: column; align-items: center; padding: 60px 20px; color: var(--muted); text-align: center; gap: 12px; }
.empty .hint { font-size: 13px; }
.list { list-style: none; padding: 0; margin: 16px 0 0; }
.item { background: var(--panel); border: 1px solid var(--border); border-radius: var(--radius); padding: 12px; margin-bottom: 10px; }
.head-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.name { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-size: 14px; }
.ico.spin { animation: spin 1s linear infinite; color: var(--accent); }
@keyframes spin { to { transform: rotate(360deg); } }
.ico.ok { color: var(--success); }
.ico.err { color: var(--danger); }
.ico.paused { color: var(--muted); }
.ico.muted { color: var(--muted); }
.bar { height: 4px; background: var(--panel-2); border-radius: 2px; overflow: hidden; margin-bottom: 8px; }
.fill { height: 100%; background: var(--accent); transition: width 0.3s; }
.fill.ok { background: var(--success); }
.fill.err { background: var(--danger); }
.row { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: var(--muted); flex-wrap: wrap; gap: 8px; }
.actions { display: flex; gap: 4px; }
.actions button { background: transparent; border: none; padding: 4px; color: var(--text); display: inline-flex; align-items: center; gap: 4px; font-size: 12px; }
</style>
