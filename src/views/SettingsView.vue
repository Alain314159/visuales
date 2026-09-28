<script setup>
import { ref } from 'vue';
import { getBaseUrl, setBase, health } from '../lib/api';

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

    <label style="display: block; margin-top: 16px; margin-bottom: 6px; color: var(--muted); font-size: 14px;">
      URL base
    </label>
    <input v-model="input" placeholder="https://visuales.uclv.cu" />
    <p style="color: var(--muted); font-size: 13px; margin-top: 6px;">
      Por defecto: <code>https://visuales.uclv.cu</code>
    </p>

    <div style="margin-top: 16px; display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
      <button class="primary" @click="save">Guardar</button>
      <button @click="test" :disabled="testing">{{ testing ? 'Probando...' : 'Probar conexion' }}</button>
      <span v-if="saved" style="color: var(--success);">Guardado</span>
    </div>

    <p v-if="testResult && testResult.ok" style="color: var(--success); margin-top: 12px;">
      Conexion OK (HTTP {{ testResult.status }})
    </p>
    <p v-else-if="testResult && !testResult.ok" style="color: var(--danger); margin-top: 12px;">
      {{ testResult.error || ('HTTP ' + testResult.status) }}
    </p>
  </div>
</template>
