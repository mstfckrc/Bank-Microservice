# Kimlik ve güvenlik akışı

Bu şema giriş ve oturum açılmış kullanıcının korunan API isteğini özetler; kayıt,
çıkış ve servisler arası tüm yetki akışlarının ayrıntılı dökümü değildir.

[PNG görüntüsü](kimlik-ve-guvenlik.png)

```mermaid
---
config:
  theme: base
  sequence:
    mirrorActors: true
    actorMargin: 45
    messageMargin: 24
    noteMargin: 16
    diagramMarginX: 28
    diagramMarginY: 20
    width: 170
    height: 64
    actorFontSize: 15
    actorFontWeight: 600
    messageFontSize: 14
    noteFontSize: 15
    noteFontWeight: 600
    wrap: false
    useMaxWidth: false
  themeVariables:
    darkMode: false
    background: "#ffffff"
    primaryColor: "#eff6ff"
    primaryTextColor: "#0f172a"
    primaryBorderColor: "#93c5fd"
    actorBkg: "#eff6ff"
    actorBorder: "#93c5fd"
    actorTextColor: "#0f172a"
    actorLineColor: "#b8c5d6"
    signalColor: "#52647d"
    signalTextColor: "#26364c"
    noteBkgColor: "#e8eef8"
    noteTextColor: "#1e3a5f"
    noteBorderColor: "#e8eef8"
    fontFamily: "Arial, sans-serif"
  themeCSS: |
    .actor-line { stroke: #64748b !important; stroke-width: 1.4px !important; stroke-dasharray: 5 5 !important; opacity: 0.85; }
    rect.actor { rx: 12px; ry: 12px; stroke-width: 1.5px; }
    rect.actor[name="Kullanici"] { fill: #f1f5f9 !important; stroke: #94a3b8 !important; }
    rect.actor[name="Next"] { fill: #e0edff !important; stroke: #60a5fa !important; }
    rect.actor[name="KC"] { fill: #f0e7ff !important; stroke: #b49ae3 !important; }
    rect.actor[name="GW"] { fill: #fff1d6 !important; stroke: #e7b65a !important; }
    rect.actor[name="Servis"] { fill: #dcf5eb !important; stroke: #5bbb9a !important; }
    .messageLine0, .messageLine1 { stroke-width: 1.4px !important; }
    rect.note { rx: 8px; ry: 8px; }
---
sequenceDiagram
    participant Kullanici as Kullanıcı<br/>Tarayıcı
    participant Next as Next.js<br/>Uygulama sunucusu
    participant KC as Keycloak<br/>Kimlik sağlayıcısı
    participant GW as API Gateway<br/>İstek yönlendirme
    participant Servis as Banka servisi<br/>İşlem ve yetki

    rect rgb(246, 249, 254)
        Note over Kullanici,Servis: 01 — GİRİŞ VE OTURUM
        Kullanici->>Next: Giriş başlat
        Next-->>Kullanici: Keycloak'a yönlendir
        Kullanici->>KC: TCKN / VKN ve şifre
        KC-->>Kullanici: Giriş koduyla geri yönlendir
        Kullanici->>Next: Giriş kodunu ilet
        Next->>KC: Giriş koduyla token al
        KC-->>Next: Token yanıtı
        Next->>GW: Token ile profil iste
        GW->>Servis: Profil isteğini ilet
        Servis-->>Next: Profil yanıtı (Gateway üzerinden)
        Next-->>Kullanici: Şifreli oturum çerezleri
    end

    rect rgb(246, 252, 249)
        Note over Kullanici,Servis: 02 — GİRİŞ SONRASI İŞLEM
        Kullanici->>Next: Çerezle işlem iste
        Next->>GW: Erişim token'ıyla ilet
        GW->>GW: Token ve yetki kontrolü
        GW->>Servis: Token'ı koruyarak ilet
        Servis->>Servis: Token ve erişim kontrolü
        Servis-->>GW: İşlem yanıtı
        GW-->>Next: İşlem yanıtı
        Next-->>Kullanici: Sonuç
    end
```

Bu sıralama diyagramında oklar işlemlerin gerçekleşme sırasını gösterir.
Giriş kodu Next.js tarafından
Keycloak'ın token endpoint'ine gönderilir; profil Gateway üzerinden Auth servisinden
alınır. Nginx'in HTTPS yönlendirmeleri genel sistem diyagramındadır.

Token'lar tarayıcı JavaScript'inin okuyacağı düz metin token cookie'leri olarak
kullanılmaz; Next.js sunucusu, oturum verisini şifreli cookie'de saklar. Çerezler
HttpOnly ve Secure niteliklerini taşır. Gateway, istemcinin gönderdiği kimlik
başlıklarını temizler ve Authorization başlığındaki token'ı korur. Next.js'in bu
sunucu katmanı BFF (Backend for Frontend) olarak da adlandırılır: tarayıcı ile API arasındaki
istekleri yönetir. Girişte state, nonce ve PKCE doğrulamaları kullanılır. CSRF koruması ve
oturum süresi gibi ayrıntılar okunabilirlik için şemada kısaltılmıştır. Açık
endpoint'ler bu korunan istek akışından farklıdır.

Kaynaklar: [BFF login](../../Bank-Demo-Frontend/app/api/auth/login/route.ts),
[callback](../../Bank-Demo-Frontend/app/api/auth/callback/route.ts),
[oturum](../../Bank-Demo-Frontend/lib/server/session.ts),
[Gateway filtresi](../../api-gateway/src/main/java/com/mustafa/filter/KeycloakHeaderFilter.java).
