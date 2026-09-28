// @visuales-notify-v1
import { LocalNotifications } from '@capacitor/local-notifications';

let permissionAsked = false;

export async function ensurePermission() {
  if (permissionAsked) return true;
  permissionAsked = true;
  try {
    const perm = await LocalNotifications.checkPermissions();
    if (perm.display !== 'granted') {
      const req = await LocalNotifications.requestPermissions();
      return req.display === 'granted';
    }
    return true;
  } catch (e) {
    console.warn('notif perm:', e);
    return false;
  }
}

export async function notifyProgress(id, title, progress) {
  try {
    await LocalNotifications.schedule({
      notifications: [{
        id: hashId(id),
        title: 'Descargando: ' + title,
        body: progress + '%',
        ongoing: true,
        autoCancel: false,
        progress: { max: 100, current: progress }
      }]
    });
  } catch (e) { /* silencio */ }
}

export async function notifyDone(id, title) {
  try {
    await LocalNotifications.cancel({ notifications: [{ id: hashId(id) }] });
    await LocalNotifications.schedule({
      notifications: [{
        id: hashId(id) + 1,
        title: '✅ Descarga completada',
        body: title,
        autoCancel: true
      }]
    });
  } catch (e) { /* silencio */ }
}

function hashId(str) {
  let h = 0;
  for (let i = 0; i < str.length; i++) {
    h = ((h << 5) - h + str.charCodeAt(i)) | 0;
  }
  return Math.abs(h) % 100000;
}
