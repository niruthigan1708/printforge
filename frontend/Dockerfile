# PrintForge storefront image. Build from the frontend/ directory:
#   docker build --build-arg NEXT_PUBLIC_API_URL=https://api.example.com/api -t printforge-web frontend
# The API address is compiled into the browser bundle, so it is a build argument, not a runtime one.

# --- Build -----------------------------------------------------------------------------------
FROM node:20.19-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG NEXT_PUBLIC_API_URL=http://localhost:8080/api
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL \
    NEXT_TELEMETRY_DISABLED=1
RUN npm run build && npm prune --omit=dev

# --- Runtime ---------------------------------------------------------------------------------
FROM node:20.19-alpine
WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1
COPY --from=build --chown=node:node /app/package.json ./
COPY --from=build --chown=node:node /app/node_modules ./node_modules
COPY --from=build --chown=node:node /app/.next ./.next
USER node
EXPOSE 3000
CMD ["node_modules/.bin/next", "start", "-p", "3000"]
