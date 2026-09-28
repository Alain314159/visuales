<script setup>
import { ref } from 'vue';
import { getBaseUrl, setBase, health, isNative } from '../lib/api';
import { useDownloadsStore } from '../stores/downloads';
import {
  Save, Wifi, CheckCircle, XCircle, Loader2, Server, Smartphone,
  Sliders, Trash2, Globe
} from 'lucide-vue-next';

const store = useDownloadsStore();
const input = ref(getBaseUrl());
const saved = ref(false);
const testing = ref(false);
const testResult = ref(null);
const connections = ref(store.settings.connections);
const maxConcurrent = ref(store.settings.maxConcurrent);
const autoSubtitles = ref(store.settings.autoSubtitles);

function save() {
  setBase(input.value);
  store.settings.connections = connections.value;
  store.settings.maxConcurrent = maxConcurrent.value;
  store.settings.autoSubtitles = autoSubtitles.value;
  localStorage.setItem('visuales.dlSettings', JSON.stringify({
    connections: connections.value,
    maxConcurrent: maxConcurrent.value,
    autoSubtitles: autoSubtitles.value
  }));
  saved.value = true;
  setTimeout(() => { saved.value = false; }, 2000);
}

async function test() {
  save();
  testing.value = true;
  testResult.value = null;
  testResult.value = await health();
  testing.value = false;
}

async function clearAll() {
  if (!confirm('¿Borrar todas las descargas y la caché?')) return;
  const dbs = await indexedDB.databases();
  for (const d of dbs) if (d.name) indexedDB.deleteDatabase(d.name);
  localStorage.clear();
  location.reload();
}
</script>

<template>
  <div>
    <h2>Ajustes</h2>

    <div class="card">
      <div class="row"><Server :size="18" /><span class="label">URL base</span></div>
      <input v-model="input" placeholder="https://visuales.uclv.cu" />
      <p class="hint">Por defecto: https://visuales.uclv.cu</p>
    </div>

    <div class="card">
      <div class="row"><Sliders :size="18" /><span class="label">Descargas</span></div>
      <label class="opt">
        <span>Conexiones paralelas</span>
        <input type="number" min="1" max="16" v-model.number="connections" />
      </label>
      <label class="opt">
        <span>Descargas simultáneas</span>
        <input type="number" min="1" max="5" v-model.number="maxConcurrent" />
      </label>
      <label class="opt checkbox">
        <input type="checkbox" v-model="autoSubtitles" />
        <span>Descargar subtítulos automáticamente</span>
      </label>
    </div>

    <div class="card">
      <div class="row"><Smartphone :size="18" /><span class="label">Entorno</span></div>
      <p class="value">{{ isNative() ? 'App nativa (Capacitor)' : 'Navegador web' }}</p>
    </div>

    <div class="btns">
      <button class="primary" @click="save"><Save :size="18" /> Guardar</button>
      <button @click="test" :disabled="testing">
        <Wifi :size="18" /> {{ testing ? 'Probando...' : 'Probar conexión' }}
      </button>
    </div>

    <p v-if="saved" class="ok"><CheckCircle :size="16" /> Guardado</p>

    <div v-if="testResult" class="result" :class="{ ok: testResult.ok, err: !testResult.ok }">
      <CheckCircle v-if="testResult.ok" :size="18" />
      <XCircle v-else :size="18" />
      <span v-if="testResult.ok">Conexión OK (HTTP {{ testResult.status }})</span>
      <span v-else>{{ testResult.error || ('HTTP ' + testResult.status) }}</span>
    </div>

    <div class="card danger">
      <div class="row"><Trash2 :size="18" /><span class="label">Zona peligrosa</span></div>
      <button class="danger-btn" @click="clearAll">
        <Trash2 :size="16" /> Borrar todos los datos
      </button>
    </div>
  </div>
</template>

<style scoped>
.card { background: var(--panel); border: 1px solid var(--border); border-radius: var(--radius); padding: 14px; margin-top: 12px; }
.row { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.label { color: var(--muted); font-size: 13px; }
.hint { color: var(--muted); font-size: 12px; margin-top: 6px; }
.value { font-size: 14px; }
.opt { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; gap: 10px; font-size: 14px; }
.opt input[type="number"] { width: 80px; text-align: center; }
.opt.checkbox { justify-content: flex-start; }
.opt.checkbox input { width: auto; }
.btns { display: flex; gap: 8px; margin-top: 16px; flex-wrap: wrap; }
.btns button { display: inline-flex; align-items: center; gap: 6px; }
.ok { color: var(--success); margin-top: 12px; display: flex; align-items: center; gap: 6px; }
.result { display: flex; align-items: center; gap: 8px; margin-top: 16px; padding: 10px; border-radius: var(--radius); }
.result.ok { background: rgba(34, 197, 94, 0.1); color: var(--success); }
.result.err { background: rgba(239, 68, 68, 0.1); color: var(--danger); }
.danger { border-color: rgba(239, 68, 68, 0.3); }
.danger-btn { background: rgba(239, 68, 68, 0.1); color: var(--danger); border-color: rgba(239, 68, 68, 0.3); display: inline-flex; align-items: center; gap: 6px; width: 100%; justify-content: center; }
</style>
