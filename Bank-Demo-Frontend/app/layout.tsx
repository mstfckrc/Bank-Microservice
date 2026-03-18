import type { Metadata } from "next";
import "./globals.css";
import { Toaster } from "@/components/ui/sonner";

export const metadata: Metadata = {
  title: "Banking App",
  description: "Geleceğin finans dünyasına adım atın. Hızlı, güvenli ve yenilikçi bankacılık deneyimi için bizimle olun.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>
        {children}
        <Toaster position="top-center" richColors /> {/* 🚀 Konumu ve renkleri belirledik */}
      </body>
    </html>
  );
}
