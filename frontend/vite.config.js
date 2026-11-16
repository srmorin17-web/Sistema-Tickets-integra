import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    strictPort: true,
    proxy: {
      // Redirige todas las llamadas a /api hacia tu backend
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
  // IMPORTANTE para despliegue en Vercel:
  base: '/',
});