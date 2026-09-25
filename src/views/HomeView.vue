<script setup>
import { ref, onMounted } from 'vue';
import { useAppStore } from '../stores/app';
import { health, listDirectory } from '../lib/api';

const store = useAppStore();
const status = ref('idle');
const errorMsg = ref('');
const rootEntries = ref([]);

async function testConnection() {
  status.value = 'loading';
  errorMsg.value = '';
  try {
    await health();
    const data = await listDirectory('/');
    rootEntries.value = data.entries || [];
    status.value = 'ok';
  } catch (e) {
    status.value = 'error';
    errorMsg.value = e.message;
  }
}

onMounted(() => {
  if (store.workerReady) testConnection();
});
</script>

<template>
  <div>
    <h2>Catalogo</h2>

    <div v-if="!store.workerReady" class="card">
      <p>Configura la URL del Worker en Ajustes para empezar.</p>
    </div>

    <div v-else>
      <button class="primary" @click="testConnection" :disabled="status === 'loading'">
        {{ status === 'loading' ? 'Probando...' : 'Probar conexion' }}
      </button>

      <p v-if="status === 'error'" style="color: var(--danger); margin-top: 12px;">
        {{ errorMsg }}
      </p>

      <div v-if="status === 'ok'" style="margin-top: 16px;">
        <p style="color: var(--success);">Conexion OK - {{ rootEntries.length }} entradas en la raiz</p>
        <ul>
          <li v-for="e in rootEntries.slice(0, 10)" :key="e.name">
            {{ e.type === 'dir' ? 'DIR' : 'FILE' }} {{ e.name }}
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card {
  background: var(--panel);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px;
}
</style>
