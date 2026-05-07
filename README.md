# 🏦 Bank-Demo Microservice Architecture

Modern, ölçeklenebilir ve tam izole bir mikroservis mimarisi üzerine inşa edilmiş kapsamlı bankacılık ve finans yönetim sistemi. 

Bu proje, monolitik sistemlerin dağıtık mimarilere (Spring Cloud) geçişini, "Zero-Trust" güvenlik prensiplerini ve asenkron olay güdümlü (Event-Driven) haberleşmeyi sergilemek amacıyla geliştirilmiştir.

## 🚀 Teknolojik Altyapı (Tech Stack)

* **Backend:** Java 17, Spring Boot 3.x, Spring Cloud (Gateway, OpenFeign, Eureka)
* **Frontend:** Next.js 14 (App Router), TailwindCSS, Zustand
* **Veritabanı:** PostgreSQL 15 (Her mikroservis için bağımsız fiziksel veritabanı - *Database-per-service pattern*)
* **Mesajlaşma & Asenkron İşlemler:** RabbitMQ
* **Önbellek & Rate Limiting:** Redis
* **Kimlik Doğrulama & Yetkilendirme:** Keycloak (OAuth2 / OIDC)
* **Loglama & İstihbarat:** ELK Stack (Elasticsearch, Logstash, Kibana)
* **Altyapı & Dağıtım:** Docker, Docker Compose

---

## 🏗️ Mimari Şema ve Mikroservisler

Sistem, dış dünyadan gelen istekleri **API Gateway** üzerinden karşılar. Gateway, Keycloak üzerinden gelen JWT token'ları çözümler, güvenlik süzgecinden geçirir (Rate Limiting) ve arka plandaki servislere sadece yetki bilgilerini iletir.

| Servis Adı | Port | Görev |
| :--- | :--- | :--- |
| **api-gateway** | `8081` | Dış dünyaya açılan kapı, Rate Limiting, Zero-Trust kalkanı. |
| **frontend** | `3000` | Kullanıcı Vitrini (Bireysel, Kurumsal ve Admin panelleri). |
| **discovery-server** | `8761` | Eureka Service Registry (Servislerin birbirini bulması). |
| **backend (Core)** | `8080` | Karargah. Hesaplar, transferler, MASAK onayları, ELK loglama. |
| **auth-service** | `8086` | Kullanıcı kaydı, Keycloak senkronizasyonu, profil yönetimi. |
| **corporate-service** | `8085` | Kurumsal şirket yönetimi ve Otomatik Toplu Maaş dağıtımı. |
| **bill-service** | `8084` | Otomatik fatura ödeme talimatları ve dış servis entegrasyonu. |
| **currency-service** | `8083` | Canlı döviz kurları (Redis Cache Bypass mimarisi ile). |
| **notification-service** | `8082` | RabbitMQ kuyruğunu dinleyen Asenkron bildirim merkezi. |
| **keycloak** | `9090` | Güvenlik, Token üretimi ve kullanıcı yönetimi. |

> 📚 **Detaylı Mimari Bilgisi:** Her bir mikroservisin içerisindeki uç noktalar (Endpoints), DTO yapıları, veritabanı şemaları ve iş kuralları hakkında derinlemesine bilgi edinmek için ilgili klasörün içindeki `hafiza.txt` dosyalarını inceleyebilirsiniz.

---

## ⚙️ Kurulum ve Çalıştırma Adımları

### Adım 1: Ön Koşullar ve Sistem Hazırlığı
* Sisteminizde **Docker** ve **Docker Compose** kurulu olmalıdır.
* *Linux/Ubuntu Kullanıcıları İçin Zorunlu Ayar:* ELK loglama sisteminin (Elasticsearch) çalışabilmesi için işletim sisteminin RAM haritalama limitini artırmanız gerekir. Terminalde şu komutu çalıştırın:
  `sudo sysctl -w vm.max_map_count=262144`

### Adım 2: Çevre Değişkenlerinin (.env) Ayarlanması (1. Aşama)
Projeyi güvenli bir şekilde ayağa kaldırmak için şifrelerinizi belirlemeniz gerekmektedir. 

**Ana Dizin (Backend) Ayarları:**
Ana dizindeki `.env.example` dosyasının adını `.env` olarak değiştirin ve içindeki şu alanları kendi belirlediğiniz güvenli değerlerle doldurun:
* **Veritabanı Şifreleri:** `CORE_DB_PASS`, `AUTH_DB_PASS`, `BILL_DB_PASS`, `CORP_DB_PASS`, `KC_DB_PASS` (Docker içindeki PostgreSQL veritabanlarının şifreleridir).
* **Keycloak Admin Paneli:** `KC_ADMIN_USER` ve `KC_ADMIN_PASS` (Keycloak yönetici paneline girmek için belirleyeceğiniz kullanıcı adı ve şifredir).
* **RabbitMQ Şifresi:** `RMQ_PASS` (Mesaj kuyruğu şifresidir).
* **JWT Secret (`JWT_SECRET`):** Spring Boot'un iç güvenlik süreçleri için gereklidir. Mutlaka **256-bit (64 karakter) Hexadecimal** bir değer olmalıdır. *(Örnek sahte format: `1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b`)*. Kendinize ait rastgele bir 64 karakterli hex anahtarı üretip buraya yazın.
* **Google reCAPTCHA (`RECAPTCHA_SECRET`):** Google reCAPTCHA v2/v3 üzerinden aldığınız Backend doğrulama anahtarınızı buraya yapıştırın.
* *Not:* `KEYCLOAK_CLIENT_SECRET` alanını şimdilik boş bırakın, Adım 4'te dolduracağız.

**Frontend Ayarları:**
`Bank-Demo-Frontend` klasörü içindeki `.env.local.example` dosyasını `.env.local` olarak kopyalayın.
* `NEXT_PUBLIC_RECAPTCHA_SITE_KEY` alanına Google'dan aldığınız Public Site Key'i yazın.
* *Not:* Buradaki `KEYCLOAK_CLIENT_SECRET` alanını da şimdilik boş bırakın.

### Adım 3: Sistemi Ayağa Kaldırma
Tüm `.env` dosyalarınızı kaydettikten sonra ana dizinde terminali açın ve sistemi başlatın:
`docker compose up -d`
*(İmajların indirilmesi, veritabanlarının ve 10 farklı konteynerin ayağa kalkması bilgisayar hızınıza göre 2-5 dakika sürebilir. Lütfen bekleyin.)*

---

## 🔐 Adım 4: Keycloak Konfigürasyonu (Kritik Aşama)

Sistem ayağa kalktıktan sonra, mikroservislerin birbirini tanıyabilmesi için Keycloak üzerinde güvenlik ayarlarını yapmalı ve API iletişimi için "Client Secret" anahtarını üretmeliyiz.

1. **Giriş Yapın:** Tarayıcınızdan `http://localhost:9090` adresine gidin. `Administration Console` butonuna tıklayın. 2. Adımda belirlediğiniz `KC_ADMIN_USER` ve `KC_ADMIN_PASS` ile giriş yapın.
2. **Realm (Krallık) Oluşturun:**
   * Sol üst köşedeki (master yazan yerin yanındaki) oka tıklayın ve **"Create Realm"** butonuna basın.
   * Realm name alanına tam olarak `bank-realm` yazın ve "Create" butonuna basın.
3. **Client (İstemci) Oluşturun:**
   * Sol menüden **"Clients"** sekmesine tıklayın. Ardından **"Create client"** butonuna basın.
   * **Client ID:** `bank-auth-client` yazın ve "Next" deyin.
   * **Capability config:** sayfasında *Client authentication* anahtarını **ON** (Açık) konumuna getirin. *Authorization* anahtarını **ON** (Açık) konumuna getirin. "Next" ve ardından "Save" deyin.
4. **Gizli Anahtarı (Secret) Alın:**
   * Client başarıyla oluştuktan sonra üstteki sekmelerden **"Credentials"** sekmesine geçin.
   * Ekranda "Client Secret" değerini göreceksiniz (Örn: `A1b2...`). Yanındaki kopyala butonuna basarak bu şifreyi kopyalayın.
5. **Rolleri Tanımlayın:**
   * Sol menüden **"Realm roles"** sekmesine gidin ve "Create role" butonuna basarak sırayla şu 3 rolü ekleyin (Büyük harflerle yazılması zorunludur):
     * `ADMIN`
     * `RETAIL_CUSTOMER`
     * `CORPORATE_MANAGER`

### Adım 5: Sistemi Anahtarla Güncelleme ve Son Restart
Keycloak'tan kopyaladığınız o gizli "Client Secret" değerini alın ve şu iki dosyadaki ilgili yerlere yapıştırıp kaydedin:
1. Ana dizindeki `.env` dosyası içindeki `KEYCLOAK_CLIENT_SECRET=` alanına.
2. `Bank-Demo-Frontend` klasöründeki `.env.local` dosyası içindeki `KEYCLOAK_CLIENT_SECRET=` alanına.

Bu yeni şifrelerin sistem tarafından algılanması için sadece ilgili mikroservisleri yeniden başlatmamız gerekiyor. Ana dizinde şu komutu çalıştırın:
`docker restart bank_api_gateway_micro bank_auth_micro bank_frontend_micro`

🎉 **Tebrikler! Sistem tamamen kullanıma hazırdır.** Artık `http://localhost:3000` adresi üzerinden banka arayüzüne erişebilir, kayıt olabilir ve sistemi test edebilirsiniz.

---

## 🚨 Olası Sorunlar (Troubleshooting)

**Kibana "Server is not ready yet" Hatası Veriyorsa:**
Eğer sistem ayağa kalkmasına rağmen Kibana'ya erişemiyorsanız, sunucunuzun disk kapasitesi **%90'ın üzerine** çıkmış olabilir. Elasticsearch (veri koruma kalkanı gereği) disk %90 dolduğunda kendini *Salt-Okunur (Read-Only)* moda alır ve Kibana bağlantısını reddeder.
**Çözümü:**
Kullanılmayan docker imajlarını ve yetim kalmış volumeleri temizleyerek diskte yer açın:
`docker system prune -a -f`
`docker volume prune -f`
Ardından ELK servisini yeniden başlatın: `docker restart bank_elasticsearch_micro bank_kibana_micro`

---

## 📊 ELK İstihbarat Ağı (Log Görüntüleme)
Sistemdeki tüm hareketler ve mikroservis logları Logstash üzerinden toplanıp Elasticsearch'e aktarılır.
* **Kibana Paneli:** `http://localhost:5601`
* Yeni bir "Data View" oluştururken index pattern olarak `bank-logs-*` kullanın. Tüm sistemi buradan canlı izleyebilirsiniz.

---

## 🤝 Katkıda Bulunma (Contributing)
1. Bu depoyu (repository) forklayın.
2. Yeni bir özellik dalı (feature branch) oluşturun (`git checkout -b feature/YeniGelistirme`).
3. Değişikliklerinizi commit edin (`git commit -m 'Yeni bir özellik eklendi'`).
4. Dalınızı (branch) gönderin (`git push origin feature/YeniGelistirme`).
5. Pull Request (PR) oluşturun.