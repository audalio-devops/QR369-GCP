import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const VA_TARGET = process.env.VITE_VA_API_URL || 'http://localhost:8080';
const PS_TARGET = process.env.VITE_PS_API_URL || 'http://localhost:8081';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    host: true, // Necessário para possibilitar o redirecionamento de portas do Docker Container para o Host
    proxy: {
      // Redireciona requisições para o virtual-assistant (porta 8080)
      '/api': {
        target: VA_TARGET,
        changeOrigin: true,
      },
      '/chat': {
        target: VA_TARGET,
        changeOrigin: true,
      },
      '/cnpj': {
        target: VA_TARGET,
        changeOrigin: true,
      },
      // Redireciona requisições para o prospecting-service (porta 8081)
      '/prospecting': {
        target: PS_TARGET,
        changeOrigin: true,
      }
    }
  }
})

