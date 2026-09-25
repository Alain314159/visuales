<script setup>
import { RouterView, RouterLink, useRoute } from 'vue-router';
import { computed } from 'vue';

const route = useRoute();
const activeTab = computed(() => route.name);
</script>

<template>
  <div class="app">
    <header class="topbar">
      <h1>Visuales</h1>
    </header>

    <main class="content">
      <RouterView />
    </main>

    <nav class="tabbar">
      <RouterLink to="/" class="tab" :class="{ active: activeTab === 'home' }">
        <span class="icon">C</span>
        <span>Catalogo</span>
      </RouterLink>
      <RouterLink to="/descargas" class="tab" :class="{ active: activeTab === 'downloads' }">
        <span class="icon">D</span>
        <span>Descargas</span>
      </RouterLink>
      <RouterLink to="/ajustes" class="tab" :class="{ active: activeTab === 'settings' }">
        <span class="icon">A</span>
        <span>Ajustes</span>
      </RouterLink>
    </nav>
  </div>
</template>

<style scoped>
.app {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.topbar {
  padding: 12px 16px;
  padding-top: calc(12px + env(safe-area-inset-top));
  background: var(--panel);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 10;
}

.topbar h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  padding-bottom: 80px;
}

.tabbar {
  display: flex;
  background: var(--panel);
  border-top: 1px solid var(--border);
  padding-bottom: env(safe-area-inset-bottom);
}

.tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 0;
  font-size: 12px;
  color: var(--muted);
  transition: color 0.15s;
}

.tab.active { color: var(--accent); }
.tab .icon { font-size: 20px; font-weight: 700; }
</style>
