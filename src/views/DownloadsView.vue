<script setup>
import { onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { useDownloadsStore } from '../stores/downloads';
import { getDirectUrl, getFileSize } from '../lib/api';
import { Download, Pause, Play, Trash2, Check, X, Loader2 } from 'lucide-vue-next';

const route = useRoute();
const store = useDownloadsStore();

onMounted(async () => {
  await store.load();
  if (route.query.url && route.query.name) {
    await startDownload(route.query.url, route.query.name, parseInt(route.query.size || '0', 10));
  }
});

async function startDownload(path, name, size) {
  const url = getDirectUrl(path);
  const id = await store.add({ url, name, path, size });
  // Aquí en la próxima fase conectamos el downloader nativo
  // Por ahora solo registramos la intención
}

function formatSize(bytes) {
  if (!bytes) return '—';
  if (bytes >= 1073741824) return (bytes / 1073741824).toFixed(2) + ' GB';
  if (bytes >= 1048576) return (bytes / 1048576).toFixed(1) + ' MB';
  if (bytes >= 1024) return (bytes / 1024).toFixed(0) + ' KB';
  return bytes + ' B';
}

const all = computed(() => store.items);
</script>

<template>
  <div>
    <h2>Descargas</h2>

    <p v-if="!all.length" class="empty">Aún no hay descargas. Ve al catálogo y pulsa en una película o serie.</p>

    <ul v-else class="list">
      <li v-for="d in all" :key="d.id" class="item">
        <div class="head">
          <Loader2 v-if="d.status === 'downloading'" :size="20" class="spin" />
          <Check v-else-if="d.status === 'completed'" :size="20" class="ok" />
          <X v-else-if="d.status === 'failed'" :size="20" class="err" />
          <Pause v-else-if="d.status === 'paused'" :size="20" class="paused" />
          <Download v-else :size="20" class="muted" />
          <span class="name">{{ d.name }}</span>
        </div>
        <div class="bar">
          <div class="fill" :style="{ width: (d.progress || 0) + '%' }"></div>
        </div>
        <div class="row">
          <span class="size">{{ formatSize(d.size) }} · {{ d.progress || 0 }}%</span>
          <div class="actions">
            <button v-if="d.status === 'paused'" @click="() => {}"><Play :size="16" /></button>
            <button v-else-if="d.status === 'downloading'" @click="() => {}"><Pause :size="16" /></button>
            <button @click="store.remove(d.id)"><Trash2 :size="16" /></button>
          </div>
        </div>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.empty { color: var(--muted); text-align: center; padding: 40px 20px; }
.list { list-style: none; padding: 0; margin: 0; }
.item { background: var(--panel); border: 1px solid var(--border); border-radius: var(--radius); padding: 12px; margin-bottom: 10px; }
.head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.name { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-size: 14px; }
.spin { animation: spin 1s linear infinite; color: var(--accent); }
@keyframes spin { to { transform: rotate(360deg); } }
.ok { color: var(--success); }
.err { color: var(--danger); }
.paused { color: var(--muted); }
.muted { color: var(--muted); }
.bar { height: 4px; background: var(--panel-2); border-radius: 2px; overflow: hidden; margin-bottom: 8px; }
.fill { height: 100%; background: var(--accent); transition: width 0.3s; }
.row { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: var(--muted); }
.actions button { background: transparent; border: none; padding: 4px; }
</style>
