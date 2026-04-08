import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';

export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const code = searchParams.get('code');

  // 🚀 DÜZELTME 1: Docker 0.0.0.0 hatasını önlemek için sabit ana adres
  const baseUrl = 'http://localhost:3000';

  if (!code) {
    return NextResponse.redirect(new URL('/login?error=code_missing', baseUrl));
  }

  try {
    // 🚀 DÜZELTME 2: Docker içindeyken iç ağ adresini (keycloak:8080) kullan
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
        // DİKKAT: redirect_uri KESİNLİKLE http://localhost:3000 kalmalı!
        redirect_uri: 'http://localhost:3000/api/auth/callback', 
      }),
    });

    const tokenData = await tokenResponse.json();

    if (!tokenResponse.ok) {
      throw new Error(tokenData.error_description || "Token alınamadı");
    }

    const accessToken = tokenData.access_token;
    const idToken = tokenData.id_token; // 🚀 YENİ: Çıkış yaparken bize lazım olacak asıl bilet!

    // 🚀 DÜZELTME 3: Next.js 15 kuralları gereği await ile çerezi bekleme
    const cookieStore = await cookies();
    
    // 1. Giriş Biletini (Access Token) Kaydet
    cookieStore.set({
      name: 'token',
      value: accessToken,
      httpOnly: false, 
      path: '/',
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
      maxAge: 60 * 60 * 24, // 1 gün
    });

    // 2. Çıkış Biletini (ID Token) Kaydet
    if (idToken) {
      cookieStore.set({
        name: 'id_token',
        value: idToken,
        httpOnly: false, 
        path: '/',
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'lax',
        maxAge: 60 * 60 * 24,
      });
    }

    // 3. Adamı Senkronizasyon Odasına yönlendiriyoruz! (Sabit baseUrl ile)
    return NextResponse.redirect(new URL('/auth-sync', baseUrl));

  } catch (error) {
    console.error("🚨 Keycloak Callback Hatası:", error);
    // Hata durumunda da adamı 0.0.0.0 boşluğuna düşürmeyip sabit adrese yolluyoruz
    return NextResponse.redirect(new URL('/login?error=auth_failed', baseUrl));
  }
}