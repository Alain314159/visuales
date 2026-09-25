import { defineStore } from 'pinia';

const STORAGE_KEY = 'visuales.app.v1';

function load() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}');
  } catch {
    return {};
  }
}

export const useAppStore = defineStore('app', {
  state: () => {
    const saved = load();
    return {
      workerUrl: saved.workerUrl || ''
    };
  },
  getters: {
    workerReady: (state) => !!state.workerUrl
  },
  actions: {
    setWorkerUrl(url) {
      this.workerUrl = (url || '').trim().replace(/\/$/, '');
      this.persist();
    },
    persist() {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        workerUrl: this.workerUrl
      }));
    }
  }
});
