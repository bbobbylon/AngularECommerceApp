# syntax=docker/dockerfile:1
# --- Build stage: produce the static Angular bundle ---
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# The API URL is baked in at build time. Override per environment:
#   docker build --build-arg API_URL=https://your-backend-host/api ...
ARG API_URL=http://localhost:8585/api
RUN sed -i "s#http://localhost:8585/api#${API_URL}#g" src/environments/environment.ts
RUN npx ng build

# --- Run stage: serve the SPA with nginx ---
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/angular-ecommerce/browser /usr/share/nginx/html
EXPOSE 80
