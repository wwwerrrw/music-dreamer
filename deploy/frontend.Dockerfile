FROM node:24.18.0-bookworm-slim AS build

WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
COPY frontend/shared/package.json ./shared/package.json
COPY frontend/music-web/package.json ./music-web/package.json
COPY frontend/admin-web/package.json ./admin-web/package.json
RUN npm ci

COPY frontend/shared ./shared
COPY frontend/music-web ./music-web
COPY frontend/admin-web ./admin-web
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /workspace/frontend/music-web/dist /usr/share/nginx/html/music
COPY --from=build /workspace/frontend/admin-web/dist /usr/share/nginx/html/admin
