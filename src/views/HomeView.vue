<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { listDirectory, getDirectUrl } from '../lib/api';
import { loadPoster } from '../lib/poster';
import {
  Folder, Film, FileText, Image, Archive, Music, File, ChevronRight,
  Home, RefreshCw, Search, Download, Loader2
} from 'lucide-vue-next';

const router = useRouter();
const currentPath = ref('/');
const entries = ref([]);
const loading = ref(false);
const error = ref('');
const search = ref('');
const posters = ref({});

const breadcrumbs = computed(() => {
  if (currentPath.value === '/') return [{ name: 'Inicio', path: '/' }];
  const parts = currentPath.value.split('/').filter(Boolean);
  const crumbs = [{ name: 'Inicio', path: '/' }];
  let acc = '';
  for (const p of parts) {
    acc += '/' + p;
    crumbs.push({ name: decodeURIComponent(p), path: acc });
  }
  return crumbs;
});

const filtered = computed(() => {
  if (!search.value) return entries.value;
  const q = search.value.toLowerCase();
  return entries.value.filter(e => e.name.toLowerCase().includes(q));
});

function iconFor(entry) {
  if (entry.type === 'dir') return Folder;
  if (entry.type === 'video') return Film;
  if (entry.type === 'image') return Image;
  if (entry.type === 'text') return FileText;
  if (entry.type === 'archive') return Archive;
  if (entry.type === 'audio') return Music;
  return File;
}

function formatSize(bytes) {
  if (!bytes) return '';
  if (bytes >= 1073741824) return (bytes / 1073741824).toFixed(2) + ' GB';
  if (bytes >= 1048576) return (bytes / 1048576).toFixed(1) + ' MB';
  if (bytes >= 1024) return (bytes / 1024).toFixed(0) + ' KB';
  return bytes + ' B';
}

async function load(path) {
  loading.value = true;
  error.value = '';
  try {
    const data = await listDirectory(path);
    entries.value = data.entries.filter(e => e.type !== 'parent');
    currentPath.value = path;
    search.value = '';
    posters.value = {};
    for (const e of entries.value) {
      if (e.type === 'image' && e.name.toLowerCase().includes('poster')) {
        const parent = e.path.substring(0, e.path.lastIndexOf('/'));
        loadPoster(getDirectUrl(e.path)).then(url => {
          if (url) posters.value[parent] = url;
        });
      }
    }
  } catch (e) {
    error.value = String(e.message || e);
  } finally {
    loading.value = false;
  }
}

function findSubtitle(videoPath) {
  const base = videoPath.replace(/\.[^.]+$/, '');
  return entries.value.find(e =>
    e.type === 'text' && e.name.toLowerCase().endsWith('.srt') &&
    e.path.replace(/\.[^.]+$/, '') === base
  );
}

function openEntry(entry) {
  if (entry.type === 'dir') {
    load(entry.path);
  } else {
    const sub = entry.type === 'video' ? findSubtitle(entry.path) : null;
    router.push({
      name: 'downloads',
      query: {
        url: entry.path,
        name: entry.name,
        size: entry.size,
        sub: sub ? sub.path : ''
      }
    });
  }
}

onMounted(() => { load('/'); });
</script>

<template>
  <div>
    <div class="crumbs">
      <button v-for="(c, i) in breadcrumbs" :key="c.path" class="crumb" @click="load(c.path)">
        <Home v-if="i === 0" :size="14" />
        <ChevronRight v-else :size="12" class="sep" />
        <span>{{ c.name }}</span>
      </button>
    </div>

    <div class="searchbar">
      <Search :size="18" />
      <input v-model="search" placeholder="Buscar aquí..." />
      <button @click="load(currentPath)" :disabled="loading" class="refresh">
        <RefreshCw :size="18" :class="{ spin: loading }" />
      </button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <div v-if="loading" class="loading"><Loader2 :size="32" class="spin" /></div>

    <ul v-else class="entries">
      <li v-for="e in filtered" :key="e.path" @click="openEntry(e)" class="entry">
        <div v-if="e.type === 'dir' && posters[e.path]" class="poster-wrap">
          <img :src="posters[e.path]" class="poster" loading="lazy" />
          <Folder :size="18" class="folder-badge" />
        </div>
        <component v-else :is="iconFor(e)" :size="22" class="ico" :class="e.type" />
        <div class="info">
          <span class="name">{{ e.name }}</span>
          <span class="meta">{{ e.modified }}<template v-if="e.size"> · {{ formatSize(e.size) }}</template></span>
        </div>
        <Download v-if="e.type !== 'dir'" :size="16" class="dl" />
        <ChevronRight v-else :size="18" class="chev" />
      </li>
    </ul>
  </div>
</template>

<style scoped>
.crumbs { display: flex; flex-wrap: wrap; align-items: center; gap: 2px; margin-bottom: 12px; font-size: 13px; }
.crumb { display: inline-flex; align-items: center; gap: 4px; background: transparent; border: none; color: var(--accent); padding: 4px 6px; font-size: 13px; }
.crumb .sep { color: var(--muted); }
.searchbar { display: flex; align-items: center; gap: 8px; background: var(--panel); border: 1px solid var(--border); border-radius: var(--radius); padding: 6px 12px; margin-bottom: 16px; }
.searchbar input { background: transparent; border: none; padding: 6px 0; }
.searchbar .refresh { background: transparent; border: none; padding: 4px; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.loading { display: flex; justify-content: center; padding: 40px; color: var(--accent); }
.error { color: var(--danger); margin-bottom: 12px; }
.entries { list-style: none; padding: 0; margin: 0; }
.entry { display: flex; align-items: center; gap: 12px; padding: 12px 8px; border-bottom: 1px solid var(--border); cursor: pointer; }
.entry:active { background: var(--panel); }
.poster-wrap { position: relative; width: 44px; height: 60px; flex-shrink: 0; border-radius: 6px; overflow: hidden; background: var(--panel-2); }
.poster { width: 100%; height: 100%; object-fit: cover; }
.folder-badge { position: absolute; bottom: 2px; right: 2px; color: white; background: rgba(0,0,0,0.6); border-radius: 3px; padding: 1px; }
.ico { flex-shrink: 0; color: var(--muted); }
.ico.dir { color: var(--accent); }
.ico.video { color: #f472b6; }
.ico.image { color: #a78bfa; }
.info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.name { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.meta { font-size: 12px; color: var(--muted); }
.chev, .dl { color: var(--muted); flex-shrink: 0; }
</style>
