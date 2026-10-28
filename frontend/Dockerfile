# Front Angular para Docker: se compila una vez y lo sirve nginx.
# nginx también reenvía /api al backend, así el front usa rutas relativas
# (/api/v1) igual que con el proxy de `ng serve` y no hace falta CORS.

# Angular 22 pide Node 22.22.3+ o 24.15+.
FROM node:24-alpine AS build
WORKDIR /app

# Primero solo las dependencias, para que esta capa quede cacheada.
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
