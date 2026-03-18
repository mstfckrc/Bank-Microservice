import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  devIndicators: false, // Geliştirme modunda sayfa yenileme göstergesini devre dışı bırakır
  output: "standalone", // 🚀 DOCKER İÇİN HAYATİ AYAR: Sadece gerekli dosyaları paketler
};

export default nextConfig;