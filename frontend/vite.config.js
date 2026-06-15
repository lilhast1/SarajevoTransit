import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const currentDir = path.dirname(fileURLToPath(import.meta.url))
  const envDir = path.resolve(currentDir, '..')
  const env = loadEnv(mode, envDir, '')

  return {
    envDir,
    plugins: [react()],
    server: {
      proxy: {
        // Forward all /api/** requests to the API gateway (avoids CORS in dev)
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        '/notifications': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        '/subscriptions': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        // Temporary dev proxy for live vehicle positions from javniprevozks.ba
        '/jp-api': {
          target: 'https://javniprevozks.ba',
          changeOrigin: true,
          secure: true,
          rewrite: (requestPath) => requestPath.replace(/^\/jp-api/, ''),
          configure(proxy) {
            proxy.on('proxyReq', (proxyReq) => {
              if (env.JP_API_TOKEN) {
                proxyReq.setHeader('Authorization', `Bearer ${env.JP_API_TOKEN}`)
              }
              proxyReq.setHeader('Accept', 'application/json')
            })
          },
        },
      },
    },
    test: {
      environment: 'jsdom',
      globals: true,
      setupFiles: ['./src/test/setup.js'],
    },
  }
})
