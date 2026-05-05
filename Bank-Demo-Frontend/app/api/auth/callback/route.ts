import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';

export async function GET(request: Request) {

  // 🚀 BUKALEMUN YAPI: İsteğin geldiği asıl adresi (localhost veya 192.x) dinamik olarak yakala!
  const requestUrl = new URL(request.url);
  const baseUrl = process.env.NEXT_PUBLIC_FRONTEND_URL || "http://localhost:3000";
  const code = requestUrl.searchParams.get('code');

  if (!code) {
    return NextResponse.redirect(new URL('/login?error=code_missing', baseUrl));
  }

  try {
    const internalKeycloakUrl = process.env.KEYCLOAK_INTERNAL_URL || process.env.NEXT_PUBLIC_KEYCLOAK_URL;

    // 1. Şifreyi kullanarak Keycloak'tan Gerçek Token'ı alıyoruz!
    const tokenResponse = await fetch(`${internalKeycloakUrl}/realms/${process.env.KEYCLOAK_REALM}/protocol/openid-connect/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'authorization_code',
        client_id: process.env.KEYCLOAK_CLIENT_ID!,
        client_secret: process.env.KEYCLOAK_CLIENT_SECRET!,
        code: code,
        // 🚀 BUKALEMUN YAPI: İlk gönderdiğimiz biletle birebir aynı dinamik adresi veriyoruz!
        redirect_uri: `${baseUrl}/api/auth/callback`,
      }),
    });

    const tokenData = await tokenResponse.json();

    if (!tokenResponse.ok) {
      throw new Error(tokenData.error_description || "Token alınamadı");
    }

    const accessToken = tokenData.access_token;
    const idToken = tokenData.id_token; 

    const cookieStore = await cookies();
    
    cookieStore.set({
      name: 'token',
      value: accessToken,
      httpOnly: false, 
      path: '/',
      secure: false,
      sameSite: 'lax',
      maxAge: 60 * 60 * 24, 
    });

    if (idToken) {
      cookieStore.set({
        name: 'id_token',
        value: idToken,
        httpOnly: false, 
        path: '/',
        secure: false,
        sameSite: 'lax',
        maxAge: 60 * 60 * 24,
      });
    }

    // 3. Adamı Senkronizasyon Odasına yönlendiriyoruz! 
    return NextResponse.redirect(new URL('/auth-sync', baseUrl));

  } catch (error) {
    console.error("🚨 Keycloak Callback Hatası:", error);
    // Hata olsa bile artık bizi 0.0.0.0 veya localhost'a değil, doğru IP'ye atacak!
    return NextResponse.redirect(new URL('/login?error=auth_failed', baseUrl));
  }
}
