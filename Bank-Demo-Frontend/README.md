# Bank-Demo Frontend

Next.js 16 ve TypeScript ile bireysel müşteri, kurumsal müşteri ve yönetici
ekranlarını sunar. Ana proje ve kurulum bilgileri için [kök README](../README.md)
ve [yerel kurulum rehberini](../docs/yerel-kurulum.md) kullanın.

## İstek ve oturum akışı

Tarayıcı API isteklerini aynı origin altındaki `/api/bank` yoluna gönderir.
Next.js sunucusu oturum çerezlerini okur ve erişim token'ını Gateway isteğine ekler.
Giriş Keycloak üzerinden Authorization Code + PKCE ile yürür.

Oturum verisi şifreli, HttpOnly ve Secure çerezlerde saklanır. Zustand arayüzün
profil ve oturum durumunu tutar; tarayıcı JavaScript'i token'ları cookie'den okuyup
Authorization başlığına eklemez.

[Kimlik ve güvenlik diyagramı](../docs/mimari/kimlik-ve-guvenlik.md)

## Kaynak düzeni

| Dizin | İçerik |
| --- | --- |
| `app/` | Sayfalar, ortak yerleşimler ve sunucu API route'ları |
| `lib/server/` | Oturum, OIDC ve Gateway bağlantısı |
| `components/` | Ekran bileşenleri ve ortak modallar |
| `hooks/` ve `services/` | Sayfa işlemleri ve API çağrıları |
| `store/` ve `types/` | Arayüz durumu ve TypeScript tipleri |

## Yapılandırma

Desteklenen yerel akış repository kökündeki Docker Compose dosyasını ve `.env`
dosyasını kullanır. Bu modül için ayrı `.env.local` hazırlanmaz.

`NEXT_PUBLIC_*` ayarları build sırasında tarayıcı paketine girer. BFF oturum
anahtarı ve client secret gibi özel değerler Compose tarafından runtime'da verilir.
Public ayar değişikliğinde frontend yeniden build edilmelidir.
