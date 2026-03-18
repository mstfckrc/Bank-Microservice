# 1. AŞAMA: Bağımlılıkları Kurma (Deps)
FROM node:20-alpine AS deps
WORKDIR /app
# npm ci kullanabilmek için package.json ve package-lock.json (varsa) kopyalıyoruz
COPY package.json package-lock.json* ./
RUN npm ci

# 2. AŞAMA: Derleme (Builder)
FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# 🚀 Backend'e atılacak isteklerin adresini ortam değişkeni olarak build anında alabiliriz
RUN npm run build

# 3. AŞAMA: Çalıştırma (Runner) - Sadece gerekli dosyaları alıp hafif bir imaj yapıyoruz
FROM node:20-alpine AS runner
WORKDIR /app

ENV NODE_ENV production
# 🚀 İŞTE SENİN BAHSETTİĞİN O KRİTİK AYAR: Host'u 0.0.0.0 yapıyoruz ki dışarıdan erişilebilsin!
ENV HOSTNAME "0.0.0.0"
ENV PORT 3000

# Standalone modunda üretilen dosyaları kopyalıyoruz
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

EXPOSE 3000

# Next.js standalone sunucusunu başlatıyoruz
CMD ["node", "server.js"]