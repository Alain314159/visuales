import { createRouter, createWebHashHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import DownloadsView from '../views/DownloadsView.vue';
import SettingsView from '../views/SettingsView.vue';

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/descargas', name: 'downloads', component: DownloadsView },
  { path: '/ajustes', name: 'settings', component: SettingsView }
];

export default createRouter({
  history: createWebHashHistory(),
  routes
});
