import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// ============================================================================
// VITE CONFIG — the frontend build tool (Week 2: Frontend Build & Tooling).
// ============================================================================
// Vite does two jobs:
//   dev   -> a fast dev server (this file's `server` block) with hot module
//            replacement: edit a .tsx, the browser updates without a reload.
//   build -> `npm run build` runs tsc (type-check) then bundles everything
//            into static files in dist/ — what Stage 4 serves via nginx.
//
// THE PROXY — how the frontend reaches the backend in development:
// The browser loads this app from http://localhost:5173, but the API lives on
// http://localhost:8080. Direct calls would be CROSS-ORIGIN (different port =
// different origin), triggering the browser's CORS rules. Two ways out:
//   1. server-side CORS headers — stage-2's SecurityConfig already allows
//      localhost:5173 (see CorsConfigurationSource there), OR
//   2. the dev-server proxy below: the browser calls /api on ITS OWN origin
//      (no CORS at all), and Vite forwards the request to :8080 server-side.
// We use the proxy so api/client.ts can use RELATIVE urls ("/api/...") — the
// same code then works unchanged in Stage 4, where nginx proxies /api to the
// backend container. One fetch shape, every environment.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // stage-2 monolith OR stage-5 gateway
        changeOrigin: true,
      },
    },
  },
})
