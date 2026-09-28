<script setup>
import { ref } from 'vue';
import { useAppStore } from '../stores/app';

const store = useAppStore();
const input = ref(store.workerUrl);
const saved = ref(false);

function save() {
  store.setWorkerUrl(input.value);
  saved.value = true;
  setTimeout(() => { saved.value = false; }, 2000);
}
</script>

<template>
  <div>
    <h2>Ajustes</h2>

    <label style="display: block; margin-top: 16px; margin-bottom: 6px; color: var(--muted); font-size: 14px;">
      URL del Worker
    </label>
    <input v-model="input" placeholder="https://visuales-proxy.tu-usuario.workers.dev" />
    <p style="color: var(--muted); font-size: 13px; margin-top: 6px;">
      Ejemplo: https://visuales-proxy.algo.workers.dev
    </p>

    <button class="primary" style="margin-top: 16px;" @click="save">Guardar</button>
    <span v-if="saved" style="margin-left: 12px; color: var(--success);">Guardado</span>
  </div>
</template>
