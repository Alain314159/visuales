import { defineStore } from 'pinia';
import { db } from '../db';
import {
  initDownloader,
  startDownload,
  pauseDownload,
  resumeDownload,
  cancelDownload
} from '../lib/downloader';

let listenersBound = false;

export const useDownloadsStore = defineStore('downloads', {
  state: () => ({
    items: [],
    loaded: false
  }),
  getters: {
    active: (s) => s.items.filter(d => d.status === 'downloading' || d.status === 'paused'),
    completed: (s) => s.items.filter(d => d.status === 'completed'),
    failed: (s) => s.items.filter(d => d.status === 'failed')
  },
  actions: {
    bindListeners() {
      if (listenersBound) return;
      listenersBound = true;
      initDownloader({
        onProgress: (e) => this._handleProgress(e),
        onCompleted: (e) => this._handleCompleted(e),
        onFailed: (e) => this._handleFailed(e)
      });
    },

    async load() {
      if (this.loaded) return;
      this.items = await db.downloads.orderBy('createdAt').reverse().toArray();
      this.loaded = true;
      this.bindListeners();
    },

    async enqueue({ url, name, size }) {
      const id = 'dl_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8);
      const item = {
        id,
        url,
        name,
        size: size || 0,
        status: 'queued',
        progress: 0,
        bytesDownloaded: 0,
        createdAt: Date.now()
      };
      await db.downloads.add(item);
      this.items.unshift(item);

      try {
        await this.updateStatus(id, 'downloading', 0);
        await startDownload({ id, url, filename: name });
      } catch (e) {
        console.error('Error al iniciar descarga:', e);
        await this.updateStatus(id, 'failed', 0, String(e.message || e));
      }
      return id;
    },

    async pause(id) {
      await pauseDownload(id);
      await this.updateStatus(id, 'paused');
    },

    async resume(id) {
      await resumeDownload(id);
      await this.updateStatus(id, 'downloading');
    },

    async cancel(id) {
      await cancelDownload(id);
      await this.remove(id);
    },

    async remove(id) {
      this.items = this.items.filter(d => d.id !== id);
      await db.downloads.delete(id);
    },

    async updateStatus(id, status, progress, error) {
      const item = this.items.find(d => d.id === id);
      if (item) {
        item.status = status;
        if (progress !== undefined) item.progress = progress;
        if (error !== undefined) item.error = error;
      }
      const patch = { status };
      if (progress !== undefined) patch.progress = progress;
      if (error !== undefined) patch.error = error;
      if (status === 'completed') patch.completedAt = Date.now();
      await db.downloads.update(id, patch);
    },

    async _handleProgress(e) {
      const id = e.id || e.downloadId;
      const item = this.items.find(d => d.id === id);
      if (!item) return;
      const bytes = e.bytesDownloaded || e.bytes || 0;
      const total = e.contentLength || e.totalBytes || item.size || 0;
      const pct = total > 0 ? Math.round((bytes / total) * 100) : (e.progress ? Math.round(e.progress * 100) : 0);
      item.bytesDownloaded = bytes;
      item.progress = pct;
      if (total > 0 && total !== item.size) item.size = total;
      await db.downloads.update(id, { progress: pct, bytesDownloaded: bytes, size: item.size });
    },

    async _handleCompleted(e) {
      const id = e.id || e.downloadId;
      await this.updateStatus(id, 'completed', 100);
    },

    async _handleFailed(e) {
      const id = e.id || e.downloadId;
      await this.updateStatus(id, 'failed', undefined, e.error || 'Error desconocido');
    }
  }
});
