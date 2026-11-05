# syntax=docker/dockerfile:1

# --------------------------------------------------
# BUILD - Nuxt (SPA/estático) em web/
# --------------------------------------------------
FROM node:22-alpine AS build

WORKDIR /app

RUN corepack enable

COPY web/package.json web/pnpm-lock.yaml web/pnpm-workspace.yaml web/.npmrc ./
RUN pnpm install --frozen-lockfile

COPY web/ ./

# Publicado pelo nginx dos microservices em /app/finances/.
# NUXT_PUBLIC_API_URL vazio = mesma origem (window.location.origin) ->
# /finances e /users passam pelo gateway dos microservices.
ARG NUXT_APP_BASE_URL=/app/finances/
ARG NUXT_PUBLIC_API_URL=

ENV NUXT_APP_BASE_URL=$NUXT_APP_BASE_URL
ENV NUXT_PUBLIC_API_URL=$NUXT_PUBLIC_API_URL

RUN pnpm generate

# --------------------------------------------------
# RUNTIME - nginx estático
# --------------------------------------------------
FROM nginx:1.28-alpine AS runtime

COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/.output/public /usr/share/nginx/html/app/finances

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1/app/finances/ || exit 1
