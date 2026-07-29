import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000, // O servidor de desenvolvimento do frontend rodará na porta 3000
    proxy: {
      // Redireciona requisições para o virtual-assistant (porta 8080)
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/chat': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/cnpj': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Redireciona requisições para o prospecting-service (porta 8081)
      '/prospecting': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      }
    }
  }
})
