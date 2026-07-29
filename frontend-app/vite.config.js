import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000, // O servidor de desenvolvimento do frontend rodará na porta 3000
    proxy: {
      // Redireciona requisições de /api/virtual-assistant para o serviço de assistente virtual
      '/api/virtual-assistant': {
        target: 'http://localhost:8080', // Assumindo que o virtual-assistant roda na 8080
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/virtual-assistant/, '')
      },
      // Redireciona requisições de /api/prospecting para o serviço de prospecção
      '/api/prospecting': {
        target: 'http://localhost:8081', // O prospecting-service roda na 8081
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/prospecting/, '')
      }
    }
  }
})
