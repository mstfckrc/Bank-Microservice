import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get('token')?.value;
  const idToken = request.cookies.get('id_token')?.value; // Çıkış için eklendi

  const isAuthRoute = pathname.startsWith('/login') || pathname.startsWith('/register');
  const isAdminRoute = pathname.startsWith('/admin');
  const isUserRoute = pathname.startsWith('/user') || pathname.startsWith('/dashboard');
  const isCompanyRoute = pathname.startsWith('/company');

  // 1. Token Yoksa: Korumalı alanlara girişi engelle
  if (!token) {
    if (isAdminRoute || isUserRoute || isCompanyRoute) {
      return NextResponse.redirect(new URL('/login', request.url));
    }
    return NextResponse.next();
  }

  // 2. Token Varsa: Rolü ve SÜREYİ kontrol et
  try {
    const payloadBase64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const pad = payloadBase64.length % 4;
    const paddedBase64 = pad > 0 ? payloadBase64 + '='.repeat(4 - pad) : payloadBase64;
    
    const decodedJson = atob(paddedBase64);
    const payload = JSON.parse(decodedJson);
    
    // 🚀 SÜRE KONTROLÜ (Expiration Check)
    const currentTimestamp = Math.floor(Date.now() / 1000);
    if (payload.exp && payload.exp < currentTimestamp) {
      // Token ölmüş! Keycloak çıkış URL'sini hazırla
      const keycloakUrl = process.env.NEXT_PUBLIC_KEYCLOAK_URL || "http://localhost:9090";
      const redirectUri = encodeURIComponent(request.nextUrl.origin + "/"); // Anasayfaya döner
      let logoutUrl = `${keycloakUrl}/realms/bank-realm/protocol/openid-connect/logout?post_logout_redirect_uri=${redirectUri}&client_id=bank-auth-client`;
      
      if (idToken) {
        logoutUrl += `&id_token_hint=${idToken}`;
      }

      // Çerezleri sil ve doğrudan Keycloak'a fırlat
      const response = NextResponse.redirect(logoutUrl);
      response.cookies.delete('token');
      response.cookies.delete('id_token');
      return response;
    }

    // Süre dolmamışsa rollere bakmaya devam et
    const roles: string[] = payload.realm_access?.roles || [];
    const isAdmin = roles.includes("ADMIN");
    const isCorporate = roles.includes("CORPORATE_MANAGER");

    if (isAuthRoute) {
      let redirectUrl = '/user/dashboard'; 
      if (isAdmin) redirectUrl = '/admin/dashboard';
      if (isCorporate) redirectUrl = '/company/dashboard'; 
      
      return NextResponse.redirect(new URL(redirectUrl, request.url));
    }

    // --- YETKİ DUVARLARI ---
    if (isAdminRoute && !isAdmin) {
      return NextResponse.redirect(new URL('/login', request.url));
    }

    if (isCompanyRoute && !isCorporate) {
      return NextResponse.redirect(new URL('/user/dashboard', request.url));
    }

    if (isUserRoute) {
       if (isAdmin) return NextResponse.redirect(new URL('/admin/dashboard', request.url));
       if (isCorporate) return NextResponse.redirect(new URL('/company/dashboard', request.url));
    }

  } catch (error) {
    console.error("Middleware Token Çözme Hatası:", error);
    const response = NextResponse.redirect(new URL('/login', request.url));
    response.cookies.delete('token');
    response.cookies.delete('id_token');
    return response;
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    '/login', 
    '/register', 
    '/admin/:path*', 
    '/user/:path*', 
    '/dashboard/:path*', 
    '/company/:path*'
  ],
};