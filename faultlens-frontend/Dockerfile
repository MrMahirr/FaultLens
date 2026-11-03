FROM node:20-alpine AS builder

WORKDIR /app

# pnpm version comes from "packageManager" in package.json
RUN corepack enable

# Copy dependency definitions
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./

# Install dependencies
RUN pnpm install --frozen-lockfile

# NEXT_PUBLIC_* are inlined into the client bundle at BUILD time; setting them on
# the running container has no effect. Pass them as build args, e.g.
#   docker build --build-arg NEXT_PUBLIC_API_URL=https://api.example.com/api/v1 \
#                --build-arg NEXT_PUBLIC_WS_URL=wss://api.example.com/ws .
# Empty values fall back to the localhost defaults in shared/api/endpoints.ts.
ARG NEXT_PUBLIC_API_URL
ARG NEXT_PUBLIC_WS_URL
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL} \
    NEXT_PUBLIC_WS_URL=${NEXT_PUBLIC_WS_URL}

# Copy source and build
COPY . .
RUN pnpm build

# Stage 2: Runner
FROM node:20-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

# Copy necessary files from builder
COPY --from=builder /app/public ./public

# Set the correct permission for prerender cache
RUN mkdir .next
RUN chown node:node .next

# Automatically leverage output traces to reduce image size
# https://nextjs.org/docs/advanced-features/output-file-tracing
COPY --from=builder --chown=node:node /app/.next/standalone ./
COPY --from=builder --chown=node:node /app/.next/static ./.next/static

USER node

EXPOSE 3000

CMD ["node", "server.js"]
