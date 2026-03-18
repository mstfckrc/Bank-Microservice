import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get('token')?.value;

  const isAuthRoute = pathname.startsWith('/login') || pathname.startsWith('/register');
  const isAdminRoute = pathname.startsWith('/admin');
  const isUserRoute = pathname.startsWith('/user') || pathname.startsWith('/dashboard');
  const isCompanyRoute = pathname.startsWith('/company'); // 🚀 YENİ: Kurumsal rota tanımı

  // 1. Token Yoksa: Korumalı alanlara girişi engelle
  if (!token) {
    if (isAdminRoute || isUserRoute || isCompanyRoute) {
      return NextResponse.redirect(new URL('/login', request.url));
    }
    return NextResponse.next();
  }

  // 2. Token Varsa: Rolü kontrol et
  try {
    const payloadBase64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const pad = payloadBase64.length % 4;
    const paddedBase64 = pad > 0 ? payloadBase64 + '='.repeat(4 - pad) : payloadBase64;
    
    const decodedJson = atob(paddedBase64);
    const payload = JSON.parse(decodedJson);
    
    const rawRole = String(payload.role || "");
    const isAdmin = rawRole.includes("ADMIN");
    const isCorporate = rawRole.includes("CORPORATE_MANAGER"); // 🚀 YENİ: Kurumsal rolü tanı

    // Giriş/Kayıt sayfalarındayken zaten giriş yapılmışsa, yetkisine göre doğru panele at
    if (isAuthRoute) {
      let redirectUrl = '/user/dashboard'; // Varsayılan Bireysel
      if (isAdmin) redirectUrl = '/admin/dashboard';
      if (isCorporate) redirectUrl = '/company/dashboard'; // 🚀 YENİ: Kurumsal yönlendirme
      
      return NextResponse.redirect(new URL(redirectUrl, request.url));
    }

    // --- YETKİ DUVARLARI ---

    // Admin sayfasına sadece ADMIN girebilir
    if (isAdminRoute && !isAdmin) {
      return NextResponse.redirect(new URL('/login', request.url));
    }

    // Kurumsal sayfasına sadece CORPORATE_MANAGER girebilir
    if (isCompanyRoute && !isCorporate) {
      return NextResponse.redirect(new URL('/user/dashboard', request.url));
    }

    // Bireysel sayfasına ADMIN veya CORPORATE girerse onları kendi panellerine yolla
    if (isUserRoute) {
       if (isAdmin) return NextResponse.redirect(new URL('/admin/dashboard', request.url));
       if (isCorporate) return NextResponse.redirect(new URL('/company/dashboard', request.url));
    }

  } catch (error) {
    console.error("Middleware Token Çözme Hatası:", error);
    const response = NextResponse.redirect(new URL('/login', request.url));
    response.cookies.delete('token');
    return response;
  }

  return NextResponse.next();
}

// 🚀 KRİTİK GÜNCELLEME: Matcher listesine /company eklendi
export const config = {
  matcher: [
    '/login', 
    '/register', 
    '/admin/:path*', 
    '/user/:path*', 
    '/dashboard/:path*', 
    '/company/:path*' // <-- Burası eklendi!
  ],
};