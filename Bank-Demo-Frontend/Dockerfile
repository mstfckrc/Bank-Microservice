ARG NODE_IMAGE=node:24.21.0-alpine@sha256:ebfe2f90462722a7a4de65e91990e97fe0d401c70e0e762c5b53302f905ec1c1
# Bagimliliklar: lockfile zorunludur; kaynak degisikligi bu katmani bozmaz.
FROM ${NODE_IMAGE} AS deps
WORKDIR /app
COPY package.json package-lock.json ./
# Cache indirilen paketler icindir; node_modules yine npm ci ile kurulur.
RUN --mount=type=cache,id=bank-demo-npm-node24,target=/root/.npm,sharing=locked \
    npm ci

# Derleme: yalnizca uygulama kaynaklari ve image icinde kurulan bagimliliklar.
FROM ${NODE_IMAGE} AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# NEXT_PUBLIC_* degerleri tarayici koduna build sirasinda yazilir; secret degildir.
# Degisiklikleri build'i yeniler, bagimlilik kurulum katmanini bozmaz.
# Keycloak client secret burada ARG/ENV olarak tanimlanmaz.
ARG NEXT_PUBLIC_FRONTEND_URL
ARG NEXT_PUBLIC_API_URL
ARG NEXT_PUBLIC_KEYCLOAK_URL
ARG NEXT_PUBLIC_RECAPTCHA_SITE_KEY
RUN : "${NEXT_PUBLIC_FRONTEND_URL:?Required public build setting}" \
      "${NEXT_PUBLIC_API_URL:?Required public build setting}" \
      "${NEXT_PUBLIC_KEYCLOAK_URL:?Required public build setting}" \
      "${NEXT_PUBLIC_RECAPTCHA_SITE_KEY:?Required public build setting}" \
    && npm run build

# Calistirma: yalnizca Next.js standalone sunucusu ve statik build ciktisi.
FROM ${NODE_IMAGE} AS runner
WORKDIR /app

ENV NODE_ENV=production
# Container agindan erisim icin tum arayuzlerde dinle.
ENV HOSTNAME="0.0.0.0"
ENV PORT=3000

# Projede public/ dizini yok; mevcut standalone/static ciktilari korunur.
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

EXPOSE 3000

# Runtime secret'lari Compose environment uzerinden verilir, imaja yazilmaz.
CMD ["node", "server.js"]
