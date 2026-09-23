# syntax=docker/dockerfile:1
# ============================================================================
# SECUREBANK FRONTEND IMAGE — build with Node, serve with nginx.
# ============================================================================
# A production SPA is STATIC FILES: tsc+vite compile React/TS down to plain
# HTML/CSS/JS in dist/. Serving them needs no Node at runtime — nginx does it
# faster, with less memory, and (critically) it can proxy /api to the backend
# so the browser only ever talks to ONE origin (no CORS in production, same
# as the Vite dev proxy in development — see vite.config.ts).
#
# Node appears only in the build stage and is thrown away. Final image:
# nginx:alpine (~20 MB) + ~1 MB of dist/.
# ============================================================================

# ---------- Stage 1: build ----------
FROM node:24-alpine AS build
WORKDIR /build

# package-lock.json first: `npm ci` installs the EXACT locked dependency tree
# (reproducible builds — "works on my machine" dies here). Cached until the
# lockfile changes; source edits don't re-download node_modules.
COPY package.json package-lock.json ./
RUN npm ci --no-fund --no-audit

COPY . .
RUN npm run build          # tsc -b (type-check) && vite build -> dist/

# ---------- Stage 2: serve ----------
FROM nginx:1.27-alpine AS runtime

# Custom server config: SPA routing + /api reverse proxy (see nginx.conf —
# heavily commented, it's the Week 8 "containers" lecture artifact).
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 80

# busybox wget ships with alpine — free healthcheck for compose/k8s.
HEALTHCHECK --interval=15s --timeout=3s --retries=5 \
    CMD wget -qO /dev/null http://localhost/ || exit 1
