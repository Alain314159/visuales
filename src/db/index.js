import Dexie from 'dexie';

export const db = new Dexie('visuales');

db.version(1).stores({
  downloads: '++id, url, name, status, createdAt, completedAt',
  chunks: '[downloadId+index], downloadId',
  catalogCache: 'path, fetchedAt',
  settings: 'key'
});
