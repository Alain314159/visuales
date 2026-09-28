<script setup>
import { ref, onMounted } from 'vue';
import { health, listDirectory } from '../lib/api';

const status = ref('idle');
const errorMsg = ref('');
const rootEntries = ref([]);

async function testConnection() {
  status.value = 'loading';
  errorMsg.value = '';
  try {
    const h = await health();
    if (!h.ok) throw new Error(h.error || ('HTTP ' + h.status));
    const data = await listDirectory('/');
    rootEntries.value = data.entries || [];
    status.value = 'ok';
  } catch (e) {
    status.value = 'error';
    errorMsg.value = String((e && e.message) || e);
  }
}

onMounted(() => { testConnection(); });
</script>

<template>
  <div>
    <h2>Catalogo</h2>

    <button class="primary" @click="testConnection" :disabled="status === 'loading'">
      {{ status === 'loading' ? 'Cargando...' : 'Recargar' }}
    </button>

    <p v-if="status === 'error'" style="color: var(--danger); margin-top: 12px;">
      {{ errorMsg }}
    </p>

    <div v-if="status === 'ok'" style="margin-top: 16px;">
      <p style="color: var(--success);">Raiz: {{ rootEntries.length }} entradas</p>
      <ul>
        <li v-for="e in rootEntries" :key="e.path">
          {{ e.type === 'dir' ? '📁' : '📄' }} {{ e.name }}
        </li>
      </ul>
    </div>
  </div>
</template>
