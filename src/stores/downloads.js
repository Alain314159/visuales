import { defineStore } from 'pinia';
import { db } from '../db';
import {
  initDownloader,
  startDownload,
  pauseDownload,
  resumeDownload,
  cancelDownload
} from '../lib/downloader';
import { notifyProgress, notifyDone, ensurePermission } from '../lib/notify';

let listenersBound = false;
const MAX_CONCURRENT = 2;
const MAX_RETRIES = 3;

export const useDownloadsStore = defineStore('downloads', {
  state: () => ({
    items: [],
    loaded: false,
    settings: { connections: 8, maxConcurrent: MAX_CONCURRENT, autoSubtitles: true }
  }),
  getters: {
    active: (s) => s.items.filter(d => d.status === 'downloading' || d.status === 'paused'),
    completed: (s) => s.items.filter(d => d.status === 'completed'),
    failed: (s) => s.items.filter(d => d.status === 'failed'),
    queued: (s) => s.items.filter(d => d.status === 'queued'),
    downloading: (s) => s.items.filter(d => d.status === 'downloading')
  },
  actions: {
    bindListeners() {
      if (listenersBound) return;
      listenersBound = true;
      initDownloader({
        onProgress: (e) => this._onProgress(e),
        onCompleted: (e) => this._onCompleted(e),
        onFailed: (e) => this._onFailed(e)
      });
    },

    async load() {
      if (this.loaded) return;
      this.items = await db.downloads.orderBy('createdAt').reverse().toArray();
      this.loaded = true;
      this.bindListeners();
      this._processQueue();
    },

    async enqueue({ url, name, size, subtitleUrl }) {
      const id = 'dl_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8);
      const item = {
        id, url, name, size: size || 0, subtitleUrl: subtitleUrl || null,
        status: 'queued', progress: 0, bytesDownloaded: 0,
        retries: 0, createdAt: Date.now()
      };
      await db.downloads.add(item);
      this.items.unshift(item);
      await ensurePermission();
      this._processQueue();
      return id;
    },

    async _processQueue() {
      const running = this.items.filter(d => d.status === 'downloading').length;
      const slots = this.settings.maxConcurrent - running;
      if (slots <= 0) return;
      const next = this.items.filter(d => d.status === 'queued').slice(0, slots);
      for (const item of next) {
        await this._startOne(item);
      }
    },

    async _startOne(item) {
      try {
        await this.updateStatus(item.id, 'downloading', 0);
        await startDownload({
          id: item.id,
          url: item.url,
          filename: item.name,
          connections: this.settings.connections
        });
      } catch (e) {
        await this.updateStatus(item.id, 'failed', undefined, String(e.message || e));
        this._processQueue();
      }
    },

    async pause(id) {
      await pauseDownload(id);
      await this.updateStatus(id, 'paused');
    },

    async resume(id) {
      const item = this.items.find(d => d.id === id);
      if (!item) return;
      await this.updateStatus(id, 'queued');
      this._processQueue();
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

    async _onProgress(e) {
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
      if (pct % 10 === 0) notifyProgress(id, item.name, pct);
    },

    async _onCompleted(e) {
      const id = e.id || e.downloadId;
      const item = this.items.find(d => d.id === id);
      await this.updateStatus(id, 'completed', 100);
      if (item) {
        notifyDone(id, item.name);
        if (item.subtitleUrl && this.settings.autoSubtitles) {
          this._downloadSubtitle(item);
        }
      }
      this._processQueue();
    },

    async _onFailed(e) {
      const id = e.id || e.downloadId;
      const item = this.items.find(d => d.id === id);
      if (!item) return;
      item.retries = (item.retries || 0) + 1;
      if (item.retries <= MAX_RETRIES) {
        await this.updateStatus(id, 'queued');
        setTimeout(() => this._processQueue(), 3000 * item.retries);
      } else {
        await this.updateStatus(id, 'failed', undefined, e.error || 'Error tras reintentos');
        this._processQueue();
      }
    },

    async _downloadSubtitle(item) {
      try {
        const subName = item.name.replace(/\.[^.]+$/, '.srt');
        await startDownload({
          id: item.id + '_sub',
          url: item.subtitleUrl,
          filename: subName,
          connections: 2
        });
      } catch (e) { console.warn('sub:', e); }
    }
  }
});
