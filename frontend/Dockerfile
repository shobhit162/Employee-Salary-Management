# --- build -------------------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /build

# npm ci needs only the manifests, so dependency installation caches
# independently of application source changes.
COPY package.json package-lock.json* ./
RUN npm ci

COPY . .
RUN npm run build

# --- serve -------------------------------------------------------------------
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist/salary-management/browser /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost/ >/dev/null || exit 1
