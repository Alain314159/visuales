import { defineStore } from 'pinia';
import { db } from '../db';

export const useDownloadsStore = defineStore('downloads', {
  state: () => ({
    items: [],
    loaded: false
  }),
  getters: {
    active: (state) => state.items.filter(d => d.status === 'downloading' || d.status === 'paused'),
    completed: (state) => state.items.filter(d => d.status === 'completed'),
    failed: (state) => state.items.filter(d => d.status === 'failed')
  },
  actions: {
    async load() {
      if (this.loaded) return;
      this.items = await db.downloads.orderBy('createdAt').reverse().toArray();
      this.loaded = true;
    },
    async add(download) {
      const id = await db.downloads.add({
        ...download,
        status: 'queued',
        progress: 0,
        createdAt: Date.now()
      });
      this.items.unshift({ ...download, id, status: 'queued', progress: 0, createdAt: Date.now() });
      return id;
    },
    async updateStatus(id, status, progress) {
      const item = this.items.find(d => d.id === id);
      if (item) {
        item.status = status;
        if (progress !== undefined) item.progress = progress;
      }
      await db.downloads.update(id, { status, progress });
    },
    async remove(id) {
      this.items = this.items.filter(d => d.id !== id);
      await db.downloads.delete(id);
    }
  }
});
