# Bank Microservices

Spring Cloud tabanlı, Zero-Trust güvenlik modeli ve olay güdümlü asenkron iletişim üzerine inşa edilmiş tam kapsamlı dijital bankacılık platformu.

---

## 📐 Mimari Genel Bakış

![Detaylı Yüksek Seviye Sistem Mimarisi](Detaylı%20Yüksek%20Seviye%20Sistem%20Mimarisi%20(Kuşbakışı).png)

![Yüksek Seviye Sistem Mimarisi](Yüksek%20Seviye%20Sistem%20Mimarisi%20(Kuşbakışı).png)

![Güvenlik ve Veri Süzgeci Akışı](Güvenlik%20ve%20Veri%20Süzgeci%20Akışı%20(Zero-Trust%20JWT).png)

![Keycloak Sistem Mimarisi](Keycloak%20Sistem%20Mimarisi.png)

![İç Hatlar İletişim Ağı](İç%20Hatlar%20(Endpoint%20&%20FeignClient)%20İletişim%20Ağı.png)

### İstek Akışı ve Katmanlı Mimari

Sistem, istemciden veritabanına kadar çok katmanlı bir istek akışı üzerine kurgulanmıştır. Tüm dış trafik önce **Nginx Reverse Proxy** katmanından geçer; burada SSL/TLS terminasyonu gerçekleştirilir ve `*.bank.local` alt alan adları üzerinden ilgili servislere yönlendirme yapılır. HTTP (port 80) trafiği otomatik olarak HTTPS (port 443) üzerine yönlendirilir ve TLS 1.2/1.3 protokolleri zorunlu kılınır. Nginx, istekleri **API Gateway**'e (Spring Cloud Gateway — WebFlux/Reactive) aktarır. Gateway, Keycloak'tan aldığı Public Key ile JWT token'ı doğrular, token içindeki `preferred_username` (TC/Vergi No) ve `realm_access.roles` bilgilerini çıkarır, bunları `X-Identity-Number` ve `X-User-Role` header'larına yazar ve orijinal `Authorization` header'ını siler. Arka plandaki mikroservisler Keycloak'ın varlığından haberdar değildir; yalnızca Gateway tarafından enjekte edilen sade header'lara güvenir. Bu yaklaşım, tüm iç ağ iletişimini **Zero-Trust** prensipleriyle koruma altına alır.

Servisler arası senkron iletişim **OpenFeign** istemcileri üzerinden, servis keşfi ise **Netflix Eureka** üzerinden sağlanır. Bildirimler, uyarılar ve asenkron süreçler için **RabbitMQ** kullanılır: Auth, Backend, Corporate ve Bill servisleri mesaj üretir, **Notification Service** tek tüketici olarak kuyruğu dinler ve bildirimleri işler. Bu yapı, servisleri gevşek bağlar ve yük altında darboğaz oluşumunu önler.

Gözlemlenebilirlik altyapısı iki eksende yapılandırılmıştır. **Loglama** için tüm mikroservisler Logstash'e (TCP 5000, JSON Lines) log gönderir; Logstash bu logları günlük index'ler halinde (`bank-logs-YYYY.MM.dd`) Elasticsearch'e yazar ve Kibana üzerinden sorgulanabilir kılar. **Metrik ve uyarı** için Spring Boot Actuator + Micrometer ile üretilen `/actuator/prometheus` metrikleri, Prometheus tarafından 15 saniyelik aralıklarla toplanır. Container metrikleri cAdvisor'dan, sunucu (VM) metrikleri Node Exporter'dan çekilir. Grafana bu metrikleri görselleştirir; Alertmanager, tanımlı alarm kurallarını değerlendirerek kritik durumları bildirir.

---

## 🛠 Teknoloji Altyapısı

| Kategori | Teknoloji | Kullanım Amacı |
|---|---|---|
| **Backend** | Java 17, Spring Boot 3.x, Spring Cloud (Gateway, OpenFeign, Eureka) | Mikroservis geliştirme, servis keşfi ve API yönlendirme |
| **Frontend** | Next.js (App Router), React, TypeScript, Tailwind CSS, Shadcn UI | Kullanıcı arayüzü (Bireysel, Kurumsal, Admin panelleri) |
| **Veritabanı** | PostgreSQL 15 (Her servis için fiziksel olarak izole edilmiş ayrı instance) | Kalıcı veri depolama — Database-per-service pattern |
| **Mesajlaşma** | RabbitMQ 3.13 (TopicExchange, durable queue) | Asenkron olay güdümlü bildirim iletişimi |
| **Önbellekleme** | Redis 7 | API Gateway rate limiting, döviz kuru cache (Cache-Aside) |
| **Güvenlik** | Keycloak 24.0.4 (OAuth2 / OpenID Connect), Google reCAPTCHA v2 | Kimlik doğrulama, RBAC yetkilendirme, bot koruması |
| **Loglama** | ELK Stack (Elasticsearch 8.12, Logstash 8.12, Kibana 8.12) | Merkezi log toplama, arama ve görselleştirme |
| **Metrik & Uyarı** | Prometheus, Grafana, cAdvisor, Node Exporter, Alertmanager | Container/VM metrik toplama, görselleştirme ve alarm |
| **Metrik Üretimi** | Spring Boot Actuator + Micrometer Prometheus Registry | JVM, HTTP, HikariCP, Tomcat metrikleri |
| **API Dokümantasyon** | SpringDoc OpenAPI 3 + Swagger UI (Gateway üzerinden merkezi) | Tüm servislerin endpoint dokümantasyonu |
| **Reverse Proxy** | Nginx Alpine (TLS 1.2/1.3, `*.bank.local` alt alan adları) | SSL terminasyonu, domain tabanlı yönlendirme |
| **Altyapı** | Docker, Docker Compose 3.8 | Container orkestrasyonu ve ortam yönetimi |

---

## 🏗 Mikroservisler

| Servis | Port | Sorumluluk |
|---|---|---|
| **api-gateway** | `8081` (iç) | Tek giriş noktası; JWT doğrulama, Zero-Trust header dönüşümü, Eureka tabanlı yönlendirme, Redis destekli rate limiting, merkezi Swagger UI. Dışarıya Nginx üzerinden açılır (`api.bank.local`) |
| **bank-auth-micro** | `8086` | Kullanıcı kayıt ve profil yönetimi; Keycloak entegrasyonu (kullanıcı oluşturma, rol atama); Admin müşteri CRUD; reCAPTCHA doğrulama; itiraz mekanizması |
| **Bank-Demo-Backend** | `8080` | Hesap açma/kapatma, para yatırma, EFT/Havale transferi; MASAK uyum kontrolü (≥50.000 TL onay mekanizması); toplu maaş ve fatura tahsilatı; Elasticsearch log sorgusu |
| **bank-corporate-micro** | `8085` | Kurumsal şirket yönetimi, personel CRUD, IBAN doğrulama, toplu maaş ödemesi; aylık otomatik maaş CRON scheduler (her ayın 1'i, 09:00) |
| **bill-service** | `8084` | Fatura otomatik ödeme talimatları (Elektrik, Su, Doğalgaz, İnternet, Telefon); dış kurum borç sorgulama; günlük otomatik tahsilat scheduler (02:00); çifte ödeme kalkanı |
| **currency-service** | `8083` | Döviz kuru sorgulama; iki modlu cache stratejisi: vitrin modu (Redis cache) ve canlı bypass modu (anlık hesaplama — transfer doğruluğu için) |
| **notification-service** | `8082` | RabbitMQ kuyruğunun tek tüketicisi; EMAIL, SMS, PUSH_NOTIFICATION ve SYSTEM_ALERT tiplerinde bildirim işleme |
| **discovery-server** | `8761` (iç) | Netflix Eureka Server; servis kayıt ve keşif merkezi; tüm mikroservislerin bağımlılık kökü. Dışarıya Nginx üzerinden açılır (`eureka.bank.local`) |
| **Bank-Demo-Frontend** | `3000` (iç) | Next.js (App Router) kullanıcı arayüzü; Keycloak OIDC callback ile giriş; Zustand state yönetimi; rol tabanlı erişim kontrolü (Müşteri/Kurumsal/Admin). Dışarıya Nginx üzerinden açılır (`app.bank.local`) |
| **keycloak** | `9090` | OAuth2 / OpenID Connect kimlik sunucusu; realm, client ve rol yönetimi; JWT token üretimi |
| **keycloak-db** | `5433` | Keycloak'ın kendine ait PostgreSQL 15 veritabanı |
| **postgres-core** | `5432` | Bank-Demo-Backend veritabanı (bank_core_db) |
| **postgres-auth** | `5434` | bank-auth-micro veritabanı (bank_auth_db) |
| **postgres-bill** | `5435` | bill-service veritabanı (bank_bill_db) |
| **postgres-corporate** | `5436` | bank-corporate-micro veritabanı (bank_corporate_db) |
| **elasticsearch** | `9200` | Merkezi log depolama ve tam metin arama motoru |
| **logstash** | `5000` | Log toplama pipeline'ı (TCP JSON Lines girdisi → Elasticsearch çıktısı) |
| **kibana** | `5601` | Elasticsearch üzerinde log görselleştirme ve sorgulama arayüzü |
| **rabbitmq** | `5672` / `15672` | Mesaj kuyruğu (AMQP) ve yönetim arayüzü |
| **redis** | `6379` | Önbellek motoru (rate limiting + döviz kur cache) |
| **nginx-proxy** | `80` / `443` | Reverse proxy; SSL/TLS terminasyonu, `*.bank.local` domain yönlendirmesi |
| **prometheus** | — | Metrik toplama ve 3 günlük arşivleme (iç ağdan erişim) |
| **grafana** | — | Metrik görselleştirme dashboard'ları (iç ağdan veya `monitor.bank.local` üzerinden erişim) |
| **cadvisor** | — | Docker container CPU/RAM/IO metrik toplayıcısı |
| **node-exporter** | — | Sunucu (Host VM) CPU/RAM/Disk metrik toplayıcısı |
| **alertmanager** | — | Prometheus alarm kurallarını değerlendirme ve bildirim yönlendirme |

---

## 🧠 Temel Mimari Kararlar

### Database-per-Service Pattern
Her mikroservis kendi fiziksel PostgreSQL instance'ına sahiptir (bank_core_db, bank_auth_db, bank_bill_db, bank_corporate_db). Bu izolasyon, bir servisin şema değişikliğinin diğer servisleri etkilememesini garanti eder ve servislerin bağımsız ölçeklenmesine olanak tanır.

### Asenkron İletişim — RabbitMQ
Bildirim gönderimi (e-posta, SMS, push, sistem uyarısı) gibi yan etkiler senkron istek-yanıt döngüsünden çıkarılmıştır. Üretici servisler mesajı `bank_exchange` exchange'ine bırakır, `notification-service` tek tüketici olarak `notification_queue` kuyruğunu dinler. Bu yaklaşım, üretici servisin bildirim altyapısındaki gecikme veya hatalardan etkilenmemesini sağlar ve yük altında doğal bir tampon (buffer) oluşturur.

### Önbellekleme — Redis (Two-Faced Cache Stratejisi)
Redis, iki farklı bağlamda kullanılır. **API Gateway** katmanında kayıt endpoint'ine (`/api/v1/auth/register`) uygulanan dağıtık rate limiting için Redis sayaçları tutulur. **Currency Service**'te ise "Two-Faced Cache" stratejisi uygulanır: vitrin modu (`/rates`) Redis'ten `@Cacheable` ile hızlı yanıt dönerken, canlı bypass modu (`/convert`) cache'i atlayarak anlık hesaplama yapar. Bu sayede frontend kullanıcılarına düşük gecikmeli kur bilgisi sunulurken, havale/transfer işlemlerinde anlık kur doğruluğu korunur.

### Kimlik Doğrulama — Keycloak (OIDC / OAuth2)
Özel bir authentication mekanizması geliştirmek yerine endüstri standardı Keycloak tercih edilmiştir. Bu karar, OAuth2/OpenID Connect uyumluluğu, RBAC (Role-Based Access Control) desteği, hazır kullanıcı yönetim arayüzü ve güvenlik yamalarının bağımsız olarak güncellenebilmesi avantajlarını beraberinde getirir. Üç Realm rolü (ADMIN, RETAIL_CUSTOMER, CORPORATE_MANAGER) ile tüm yetkilendirme merkezi olarak yönetilir.

### Merkezi Loglama — ELK Stack
Tüm mikroservisler loglarını Logstash'e TCP üzerinden JSON formatında gönderir. Logstash bu logları günlük index'ler (`bank-logs-YYYY.MM.dd`) halinde Elasticsearch'e yazar. Admin kullanıcıları hem Kibana arayüzünden hem de Backend'in `/api/v1/admin/logs` endpoint'i üzerinden Elasticsearch sorgulama yapabilir. Docker Compose'da healthcheck tabanlı bağımlılık zinciri (Elasticsearch → Logstash → Mikroservisler) sayesinde uygulama ayağa kalktığında hiçbir log kaybedilmez.

### Metrik ve Uyarı — Prometheus + Grafana
Her Spring Boot servisine Actuator + Micrometer Prometheus Registry entegre edilmiştir. Prometheus, tüm servislerin `/actuator/prometheus` endpoint'lerini, cAdvisor'ın container metriklerini ve Node Exporter'ın VM metriklerini 15 saniyelik aralıklarla toplar. Grafana bu metrikleri görselleştirirken, Alertmanager üç kritik alarm kuralını izler: container çökmesi (`up == 0`, 1 dk), yüksek CPU (%90+, 5 dk) ve kritik RAM seviyesi (%95+, 3 dk).

---

## 📋 Ön Koşullar

- **Docker** ve **Docker Compose** sisteminizde kurulu olmalıdır.
- **Linux Kullanıcıları İçin:** Elasticsearch'ün çalışabilmesi için sanal bellek haritalama limitinin artırılması gerekir:

```bash
sudo sysctl -w vm.max_map_count=262144
```

Bu ayarı kalıcı hale getirmek için `/etc/sysctl.conf` dosyasına `vm.max_map_count=262144` satırını ekleyin.

---

## 🚀 Kurulum

### Ortam Değişkenlerinin Ayarlanması

Proje kök dizinindeki `.env.example` dosyasını `.env` olarak kopyalayın ve gizli değerleri kendi belirlediğiniz güvenli şifrelerle doldurun:

```bash
cp .env.example .env
```

`.env` dosyasındaki zorunlu değişkenler:

```bash
# ── Ağ ve Yönlendirme ──────────────────────────────────
NEXT_PUBLIC_FRONTEND_URL=https://app.bank.local         # Frontend genel URL'si
EUREKA_URL=http://discovery-server:8761/eureka/          # Eureka servis keşif adresi
ES_URL=http://elasticsearch:9200                         # Elasticsearch iç ağ adresi
KC_INTERNAL_URL=http://keycloak:8080                     # Keycloak Docker iç ağ adresi
RMQ_HOST=rabbitmq                                        # RabbitMQ host adı
RMQ_PORT=5672                                            # RabbitMQ AMQP portu
REDIS_HOST=redis                                         # Redis host adı
REDIS_PORT=6379                                          # Redis portu

# ── Mikroservis Veritabanları ──────────────────────────
CORE_DB_URL=jdbc:postgresql://postgres-core:5432/bank_core_db
CORE_DB_USER=postgres
CORE_DB_PASS=<güçlü_şifre>                              # Core Backend veritabanı şifresi

AUTH_DB_URL=jdbc:postgresql://postgres-auth:5432/bank_auth_db
AUTH_DB_USER=postgres
AUTH_DB_PASS=<güçlü_şifre>                              # Auth Service veritabanı şifresi

BILL_DB_URL=jdbc:postgresql://postgres-bill:5432/bank_bill_db
BILL_DB_USER=postgres
BILL_DB_PASS=<güçlü_şifre>                              # Bill Service veritabanı şifresi

CORP_DB_URL=jdbc:postgresql://postgres-corporate:5432/bank_corporate_db
CORP_DB_USER=postgres
CORP_DB_PASS=<güçlü_şifre>                              # Corporate Service veritabanı şifresi

# ── Keycloak ───────────────────────────────────────────
KC_DB_URL=jdbc:postgresql://keycloak-db:5432/keycloak
KC_DB_USER=keycloak
KC_DB_PASS=<güçlü_şifre>                                # Keycloak veritabanı şifresi
KC_ADMIN_USER=admin                                      # Keycloak admin panel kullanıcı adı
KC_ADMIN_PASS=<güçlü_şifre>                             # Keycloak admin panel şifresi
KEYCLOAK_CLIENT_SECRET=                                  # Keycloak kurulumu sonrası doldurulacak
OAUTH_ISSUER_URI=https://auth.bank.local/realms/bank-realm

# ── RabbitMQ ───────────────────────────────────────────
RMQ_USER=admin
RMQ_PASS=<güçlü_şifre>                                  # RabbitMQ şifresi

# ── Güvenlik Anahtarları ───────────────────────────────
JWT_SECRET=<64_karakterli_hex_değer>                     # 256-bit hexadecimal JWT secret
RECAPTCHA_SECRET=<google_recaptcha_backend_secret>       # Google reCAPTCHA v2 backend anahtarı

# ── Grafana ────────────────────────────────────────────
GRAFANA_USER=admin
GRAFANA_PASS=<güçlü_şifre>                              # Grafana admin şifresi
```

**Frontend ortam değişkenleri** için `Bank-Demo-Frontend/.env.local.example` dosyasını `Bank-Demo-Frontend/.env.local` olarak kopyalayın ve `NEXT_PUBLIC_RECAPTCHA_SITE_KEY` ile `KEYCLOAK_CLIENT_SECRET` değerlerini girin. `KEYCLOAK_CLIENT_SECRET` alanını şimdilik boş bırakın — aşağıdaki Keycloak yapılandırması bölümünde elde edilecektir.

---

### Sistemi Başlatma

```bash
docker compose up -d
```

> Container image'larının indirilmesi, veritabanlarının başlatılması ve tüm servislerin ayağa kalkması bilgisayar performansına bağlı olarak **3–7 dakika** sürebilir. Healthcheck tabanlı bağımlılık zinciri (Elasticsearch → Logstash → Mikroservisler) sayesinde servisler doğru sırada başlatılır.

---

### Keycloak Başlangıç Yapılandırması (Zorunlu)

Sistem ayağa kalktıktan sonra, mikroservislerin kimlik doğrulama yapabilmesi için Keycloak üzerinde aşağıdaki yapılandırma adımları tamamlanmalıdır.

#### Realm Oluşturma

1. Tarayıcıdan `http://localhost:9090` adresine gidin.
2. **Administration Console** butonuna tıklayın.
3. `.env` dosyasında belirlediğiniz `KC_ADMIN_USER` ve `KC_ADMIN_PASS` ile giriş yapın.
4. Sol üst köşedeki "master" açılır menüsünden **Create Realm** butonuna tıklayın.
5. Realm name alanına `bank-realm` yazın ve **Create** butonuna basın.

#### Client Oluşturma

1. Sol menüden **Clients** → **Create client** butonuna tıklayın.
2. **Client ID** alanına `bank-auth-client` yazın ve **Next** butonuna basın.
3. **Capability config** sayfasında:
   - **Client authentication** → **ON**
   - **Authorization** → **ON**
4. **Next** → **Save** butonlarına basarak client'ı kaydedin.

#### Client Secret'ı Kopyalama

1. Oluşturulan client'ın detay sayfasında üstteki **Credentials** sekmesine tıklayın.
2. **Client Secret** değerini kopyalayın — bu değer `.env` ve `.env.local` dosyalarına yapıştırılacaktır.

#### Realm Rollerini Tanımlama

Sol menüden **Realm roles** → **Create role** butonuna tıklayarak aşağıdaki üç rolü sırasıyla oluşturun (büyük harfle yazılması zorunludur):

- `ADMIN`
- `RETAIL_CUSTOMER`
- `CORPORATE_MANAGER`

#### Secret Değerlerini Güncelleme

Kopyaladığınız Client Secret'ı şu dosyalara yapıştırın:

1. Kök dizindeki `.env` → `KEYCLOAK_CLIENT_SECRET=<kopyalanan_değer>`
2. `Bank-Demo-Frontend/.env.local` → `KEYCLOAK_CLIENT_SECRET=<kopyalanan_değer>`

Ardından ilgili servisleri yeniden başlatın:

```bash
docker restart bank_api_gateway_micro bank_auth_micro bank_frontend_micro
```

---

### Uygulamaya Erişim

#### Doğrudan Port Erişimi

Nginx'i bypass ederek doğrudan Docker host portları üzerinden erişilen servisler:

| Bileşen | Adres |
|---|---|
| Keycloak Admin Paneli | `http://localhost:9090` |
| Kibana (Log Görüntüleme) | `http://localhost:5601` |
| RabbitMQ Yönetim Paneli | `http://localhost:15672` |
| Elasticsearch API | `http://localhost:9200` |
| Bank-Demo-Backend | `http://localhost:8080` |
| bank-auth-micro | `http://localhost:8086` |
| bank-corporate-micro | `http://localhost:8085` |
| bill-service | `http://localhost:8084` |
| currency-service | `http://localhost:8083` |
| notification-service | `http://localhost:8082` |
| Logstash (TCP) | `localhost:5000` |
| Redis | `localhost:6379` |
| RabbitMQ (AMQP) | `localhost:5672` |
| PostgreSQL (Core) | `localhost:5432` |
| PostgreSQL (Auth) | `localhost:5434` |
| PostgreSQL (Bill) | `localhost:5435` |
| PostgreSQL (Corporate) | `localhost:5436` |
| PostgreSQL (Keycloak) | `localhost:5433` |

> Prometheus, Grafana, cAdvisor, Node Exporter ve Alertmanager için Docker Compose'da host portu tanımlanmamıştır. Bu servislere yalnızca Docker iç ağından veya Nginx üzerinden erişilebilir.

#### Domain Tabanlı Erişim (`*.bank.local`)

Nginx reverse proxy üzerinden SSL/TLS ile erişilen servisler:

| Domain | Hedef Servis | Açıklama |
|---|---|---|
| `https://app.bank.local` | Frontend (Next.js — port 3000) | Kullanıcı arayüzü |
| `https://api.bank.local` | API Gateway (port 8081) | Tüm API istekleri |
| `https://auth.bank.local` | Keycloak (port 8080) | Kimlik doğrulama ve OIDC |
| `https://eureka.bank.local` | Discovery Server (port 8761) | Eureka servis keşif dashboard'ı |
| `https://monitor.bank.local` | Grafana (port 3000) | Metrik görselleştirme |
| `https://docs.bank.local` | API Gateway / Swagger UI (port 8081) | Merkezi API dokümantasyonu |

> **Ön Koşul:** Domain tabanlı erişim için işletim sisteminizin `hosts` dosyasına aşağıdaki satırı ekleyin:
>
> **Windows:** `C:\Windows\System32\drivers\etc\hosts`
> **Linux/macOS:** `/etc/hosts`
>
> ```
> 127.0.0.1   app.bank.local api.bank.local auth.bank.local eureka.bank.local monitor.bank.local docs.bank.local
> ```
>
> Ayrıca SSL sertifikalarını `certs/` dizinine yerleştirmeniz gerekmektedir.

---

## 🔍 Gözlemlenebilirlik

### Loglama (ELK Stack)

Tüm mikroservisler loglarını Logstash'e TCP (port 5000) üzerinden JSON Lines formatında gönderir. Logstash bu verileri günlük index'ler halinde (`bank-logs-YYYY.MM.dd`) Elasticsearch'e yazar.

**Kibana'da logları görüntüleme:**

1. `http://localhost:5601` adresine gidin.
2. **Management** → **Stack Management** → **Data Views** → **Create data view** yolunu izleyin.
3. Index pattern olarak `bank-logs-*` girin.
4. Timestamp field olarak `@timestamp` seçin ve kaydedin.
5. **Discover** sekmesinden tüm sistem loglarını filtreleyebilir ve sorgulayabilirsiniz.

Admin kullanıcıları ayrıca Backend'in `/api/v1/admin/logs` endpoint'i üzerinden Elasticsearch'e doğrudan sorgu yaparak logları programatik olarak çekebilir.

---

### Metrikler (Prometheus + Grafana)

Prometheus, tüm hedefleri 15 saniyelik aralıklarla tarar (scrape):

| Job | Hedef | Açıklama |
|---|---|---|
| `prometheus` | `localhost:9090` | Prometheus'un kendi sağlık metrikleri |
| `cadvisor` | `bank_cadvisor_micro:8080` | Docker container CPU/RAM/IO metrikleri |
| `node_exporter` | `bank_node_exporter:9100` | Sunucu (VM) CPU/RAM/Disk metrikleri |
| `bank_microservices` | 8 Spring Boot servisi | JVM, HTTP, HikariCP, Tomcat uygulama metrikleri (`/actuator/prometheus`) |

**Grafana'ya erişim:** Nginx yapılandırması üzerinden `https://monitor.bank.local` adresinden veya Docker iç ağından container adıyla erişilebilir. Giriş bilgileri `.env` dosyasındaki `GRAFANA_USER` ve `GRAFANA_PASS` değişkenleridir.

Grafana'da yeni bir dashboard oluşturduktan sonra data source olarak Prometheus'u ekleyin (`http://bank_prometheus_micro:9090`). cAdvisor metrikleri ile container bazlı, Node Exporter metrikleri ile VM bazlı, Micrometer metrikleri ile uygulama bazlı izleme dashboard'ları kurabilirsiniz.

---

### Alertmanager

Prometheus'ta üç alarm kuralı tanımlıdır (`alert.rules.yml`):

| Alarm | Koşul | Bekleme | Seviye |
|---|---|---|---|
| **KonteynerCoktu** | `up == 0` (herhangi bir servis çevrimdışı) | 1 dakika | `critical` |
| **YuksekCpuKullanimi** | VM CPU yükü > %90 | 5 dakika | `warning` |
| **KritikRamSeviyesi** | VM RAM doluluğu > %95 | 3 dakika | `critical` |

Alertmanager, alarmları `alertname` bazlı gruplar ve varsayılan olarak bir webhook placeholder'a (`http://127.0.0.1:5000/alert-placeholder`) yönlendirir. Üretim ortamında bu adres Slack, PagerDuty veya e-posta entegrasyonuyla değiştirilmelidir.

---

## ☸️ Kubernetes Dağıtımı (K3s)

Bu proje, Docker Compose ortamına ek olarak **K3s** üzerinde 1 master + 2 worker node'dan oluşan hafif bir Kubernetes cluster'ında test edilmiştir.

### Test Ortamı

| Bileşen | Detay |
|---|---|
| Dağıtım | K3s (Lightweight Kubernetes) |
| Topoloji | 1 Master Node + 2 Worker Node |
| Ortam | Linux VM'ler üzerinde çalışan uzak sunucular |

### Deployment Yaklaşımı

Her mikroservis bağımsız bir `Deployment` ve `Service` manifest dosyası ile tanımlanmıştır. Servisler arası iletişim Kubernetes DNS çözümleme mekanizmasıyla sağlanmış; gizli değerler (veritabanı şifreleri, JWT secret, Keycloak client secret) `Secret` objeleri ile yönetilmiştir. Dış erişim için `Ingress` kaynakları yapılandırılmıştır.

> Kubernetes manifest dosyaları ve detaylı K8s kurulum dokümantasyonu hazırlanmaktadır.

---

## 🔧 Sorun Giderme

### Elasticsearch Disk Doluluğu — Salt-Okunur Mod

**Belirti:** Kibana "Server is not ready yet" hatası verir veya Elasticsearch'e veri yazılamaz.

**Neden:** Sunucu disk kapasitesi %90'ı aştığında Elasticsearch, veri bütünlüğünü korumak için kendini otomatik olarak salt-okunur (read-only) moda geçirir.

**Çözüm:**

```bash
# Kullanılmayan Docker image ve volume'ları temizleyin
docker system prune -a -f
docker volume prune -f

# Salt-okunur kilidini manuel olarak kaldırın
curl -XPUT "http://localhost:9200/_all/_settings" \
  -H 'Content-Type: application/json' \
  -d '{"index.blocks.read_only_allow_delete": null}'

# ELK servislerini yeniden başlatın
docker restart bank_elasticsearch_micro bank_logstash_micro bank_kibana_micro
```

---

### Keycloak Client Secret Uyumsuzluğu

**Belirti:** Frontend'te giriş sonrası "unauthorized" veya "invalid_client" hatası alınır.

**Neden:** `.env` dosyasındaki ve `Bank-Demo-Frontend/.env.local` dosyasındaki `KEYCLOAK_CLIENT_SECRET` değerleri Keycloak'taki güncel secret ile eşleşmiyor.

**Çözüm:** Keycloak Admin Paneli'nden (`http://localhost:9090`) client'ın **Credentials** sekmesindeki güncel secret'ı kopyalayın, her iki dosyaya da yapıştırın ve ilgili servisleri yeniden başlatın:

```bash
docker restart bank_api_gateway_micro bank_auth_micro bank_frontend_micro
```

---

### Logstash Healthcheck Zaman Aşımı

**Belirti:** Tüm mikroservisler Logstash'in ayağa kalkmasını beklerken takılı kalır.

**Neden:** Elasticsearch'ün tam olarak başlatılması uzun sürebilir; Logstash, Elasticsearch hazır olmadan healthcheck'i geçemez.

**Çözüm:** Elasticsearch'e ayrılan JVM heap belleğinin yeterli olduğunu doğrulayın (varsayılan: `ES_JAVA_OPTS=-Xms512m -Xmx512m`). Container'ların durumunu kontrol edin:

```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

Elasticsearch container'ı `healthy` durumuna geçene kadar bekleyin. Düşük bellek ortamlarında heap değerlerini artırabilirsiniz.

---

### vm.max_map_count Hatası (Linux)

**Belirti:** Elasticsearch container'ı başlatıldıktan hemen sonra çöker.

**Neden:** Linux'ta varsayılan `vm.max_map_count` (65530) Elasticsearch için yetersizdir.

**Çözüm:**

```bash
sudo sysctl -w vm.max_map_count=262144

# Kalıcı yapmak için:
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
```

---

### Container Başlatma Sırası Sorunları

**Belirti:** Bir servis başlangıçta veritabanına veya RabbitMQ'ya bağlanamıyor.

**Neden:** Docker Compose'daki `depends_on` koşulları yalnızca `service_started` seviyesinde tanımlıysa, bağımlı servis henüz hazır olmadan uygulama başlayabilir.

**Çözüm:** Sorun yaşayan container'ı tekrar başlatın:

```bash
docker restart <container_adı>
```

Spring Boot uygulamalarında `restart: on-failure` politikası tanımlı olduğundan, bağlantı kurulana kadar otomatik yeniden deneme gerçekleşir.