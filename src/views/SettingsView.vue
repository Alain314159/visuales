<script setup>
import { ref } from 'vue';
import { getBaseUrl, setBase, health, isNative } from '../lib/api';
import { Save, Wifi, CheckCircle, XCircle, Loader2, Server, Smartphone, Globe } from 'lucide-vue-next';

const input = ref(getBaseUrl());
const saved = ref(false);
const testing = ref(false);
const testResult = ref(null);

function save() {
  setBase(input.value);
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
</script>

<template>
  <div>
    <h2>Ajustes</h2>

    <div class="card">
      <div class="row">
        <Server :size="18" />
        <span class="label">URL base</span>
      </div>
      <input v-model="input" placeholder="https://visuales.uclv.cu" />
      <p class="hint">Por defecto: https://visuales.uclv.cu</p>
    </div>

    <div class="card">
      <div class="row">
        <Smartphone :size="18" />
        <span class="label">Entorno</span>
      </div>
      <p class="value">
        {{ isNative() ? 'App nativa (Capacitor)' : 'Navegador web' }}
      </p>
    </div>

    <div class="btns">
      <button class="primary" @click="save">
        <Save :size="18" /> Guardar
      </button>
      <button @click="test" :disabled="testing">
        <Wifi :size="18" />
        {{ testing ? 'Probando...' : 'Probar conexión' }}
      </button>
    </div>

    <p v-if="saved" class="ok">
      <CheckCircle :size="16" /> Guardado
    </p>

    <div v-if="testResult" class="result" :class="{ ok: testResult.ok, err: !testResult.ok }">
      <Loader2 v-if="testing" :size="18" class="spin" />
      <CheckCircle v-else-if="testResult.ok" :size="18" />
      <XCircle v-else :size="18" />
      <span v-if="testResult.ok">Conexión OK (HTTP {{ testResult.status }})</span>
      <span v-else>{{ testResult.error || ('HTTP ' + testResult.status) }}</span>
    </div>
  </div>
</template>

<style scoped>
.card { background: var(--panel); border: 1px solid var(--border); border-radius: var(--radius); padding: 14px; margin-top: 12px; }
.row { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.label { color: var(--muted); font-size: 13px; }
.hint { color: var(--muted); font-size: 12px; margin-top: 6px; }
.value { font-size: 14px; }
.btns { display: flex; gap: 8px; margin-top: 16px; }
.btns button { display: inline-flex; align-items: center; gap: 6px; }
.ok { color: var(--success); margin-top: 12px; display: flex; align-items: center; gap: 6px; }
.result { display: flex; align-items: center; gap: 8px; margin-top: 16px; padding: 10px; border-radius: var(--radius); }
.result.ok { background: rgba(34, 197, 94, 0.1); color: var(--success); }
.result.err { background: rgba(239, 68, 68, 0.1); color: var(--danger); }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
