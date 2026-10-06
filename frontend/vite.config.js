import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';

export default defineConfig({
  plugins: [svelte()],
  base: './',
  build: {
    // 构建产物直接打进 Quarkus 静态资源目录，一条命令即可启动整个系统
    outDir: '../backend/src/main/resources/META-INF/resources',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/files': 'http://localhost:8080',
    },
  },
});
