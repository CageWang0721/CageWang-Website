import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // Docker keeps the backend on an internal network. Route development
      // requests through the published nginx gateway instead of host port 8080.
      '/api': 'http://localhost',
      '/actuator': 'http://localhost'
    }
  }
})
