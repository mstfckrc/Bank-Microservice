# Yerel kurulum ve işletim

Bu rehber Docker'ın Linux/Ubuntu üzerinde çalıştığı yerel demo ortamını anlatır.
Tarayıcı aynı makinede veya başka bir bilgisayarda olabilir. Uygulama adresi
`https://app.bank.local` olarak kalır; makine farkı hosts/DNS eşlemesinde çözülür.

## 1. Ön koşullar

- Docker Engine ve Docker Compose eklentisi.
- Git, Python 3.10 veya üstü ve yerel sertifika hazırlamak için
  [mkcert](https://github.com/FiloSottile/mkcert#installation).
- Tüm stack için yeterli RAM ve boş disk. Java servisleri, ELK ve monitoring aynı
  anda çalışır; ilk image indirmeleri ve build cache ayrıca yer kaplar.
- Docker hostunda 80 ve 443 portlarının kullanılabilir olması.

Docker build'leri Java ve Node araçlarını image içinde çalıştırır. Host'a JDK veya
Node kurmak Compose akışının ön koşulu değildir.

Elasticsearch için Docker'ın çalıştığı Linux hostunda kontrol edin:

```bash
sysctl vm.max_map_count
```

Değer 262144'ten küçükse:

```bash
sudo sysctl -w vm.max_map_count=262144
```

Bu komut yeniden başlatmada kalıcı değildir. Kalıcı ayarı işletim sisteminizin
`sysctl.d` yapılandırmasıyla yönetin.

## 2. Kaynak kod ve ortam ayarları

```bash
git clone https://github.com/mstfckrc/Bank-Microservice.git
cd Bank-Microservice
python3 scripts/prepare-local-env.py
```

Hazırlık aracı kök `.env.example` üzerinden `.env` oluşturur, dosya iznini 0600
yapar ve gereken gizli değerleri rastgele üretir. Yeni boş kurulum için ilk admin
hazırlığını etkinleştirir. Üretilen değerleri terminale yazdırmaz.

`.env` zaten varsa araç onu değiştirmeden hata koduyla çıkar. Var olan kurulumu
yeniden başlatırken bu aracı tekrar çalıştırmanız gerekmez.

`.env` dosyasında şu grupları gözden geçirin:

| Ayarlar | Yapılacak işlem |
| --- | --- |
| Uygulama ve Keycloak adresleri | Mevcut `*.bank.local` adreslerini koruyun. IP farkını hosts kayıtlarında çözün. |
| Veritabanı, RabbitMQ ve Redis adresleri | Aynı Compose için servis adlarını koruyun. Bunlar tarayıcı adresleri değildir. |
| Keycloak client secret'ları | Araç aynı değerleri Keycloak importuna ve uygulama tüketicilerine verecek şekilde üretir. |
| İlk uygulama yöneticisi | `BANK_BOOTSTRAP_ADMIN_*` alanları yeni kurulumdaki demo hesabını belirler. |
| Keycloak paneli ve Grafana | İlgili USER/PASS çiftleri bu panellerin giriş bilgileridir. |
| reCAPTCHA | `NEXT_PUBLIC_RECAPTCHA_SITE_KEY` ve `RECAPTCHA_SECRET` için aynı reCAPTCHA v2 kutucuk kaydının anahtarlarını girin. |

reCAPTCHA anahtarları Google'dan alınır; rastgele secret üretimi bunların yerine
geçmez. Kayıt ekranını kullanabilmek için sağlayıcıda uygulama alan adını
yetkilendirin. İlk demo admin girişi kayıt formundan geçmez.

`NEXT_PUBLIC_*` değerleri frontend build'ine girer ve tarayıcıda görünür.
Bu alanlara parola veya client secret yazmayın. Ayrı frontend `.env.local`
dosyası kullanılmaz.

## 3. Sertifika ve tarayıcı adresleri

Mevcut çalışan kurulumda geçerli sertifikalarınız ve CA güveniniz varsa bu
oluşturma adımını tekrarlamayın. Yeni makinede Docker hostundaki repo kökünde:

```bash
mkcert -install
mkdir -p certs
mkcert -cert-file certs/bank_local.crt -key-file certs/bank_local.key bank.local '*.bank.local'
cp "$(mkcert -CAROOT)/rootCA.pem" certs/rootCA.pem
```

Compose şu dosyaları kullanır:

| Dosya | Kullanım |
| --- | --- |
| `certs/bank_local.crt` | Nginx sunucu sertifikası |
| `certs/bank_local.key` | Nginx sunucu özel anahtarı |
| `certs/rootCA.pem` | Next.js'in Keycloak HTTPS bağlantısında güvendiği CA |

Tarayıcı başka bilgisayardaysa aynı CA'nın **yalnızca public `rootCA.pem`**
dosyasını o bilgisayara taşıyıp güvenilir kök sertifika olarak yükleyin. Windows'ta
bu public dosyanın bulunduğu dizinde kullanıcı sertifika deposuna ekleme örneği:

```powershell
certutil -user -addstore Root rootCA.pem
```

`rootCA-key.pem` CA özel anahtarıdır; tarayıcı bilgisayarına veya uygulama
container'larına taşınmaz. Sertifikalar, özel anahtarlar ve `.env` Git dışında kalır.

Hosts kayıtlarını **tarayıcının bulunduğu bilgisayarda** düzenleyin. Windows'ta
PowerToys Hosts File Editor kullanılabilir. Docker aynı bilgisayardaysa:

```text
127.0.0.1 app.bank.local auth.bank.local api.bank.local docs.bank.local eureka.bank.local monitor.bank.local
```

Docker ayrı bir VM'deyse bu satırda `127.0.0.1` yerine VM'in erişilebilir IP'sini
kullanın. Örneğin mevcut laboratuvar adresi `192.168.56.10`; yeni makinede bu IP
aynı olmak zorunda değildir.

Tarayıcıdaki CA güveni ile frontend container'ının CA güveni ayrı hazırlıklardır.
Bu kurulum sertifikasız HTTP desteklemez. Nginx HTTP'yi HTTPS'e yönlendirir ve
uygulamanın oturum çerezleri HTTPS bekler.

## 4. Build ve ilk başlatma

Repo kökünde, ortam ayarları ve sertifikalar hazırken:

```bash
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

İlk komut yapılandırmayı kontrol eder. İkinci komut image'ları build eder ve
container'ları yeni image'larla başlatır. Sadece `docker compose build` çalıştırmak
mevcut container'ları güncellemez.

Kaynakları kısıtlı hostta image'ları sırayla build etmek için alternatif:

```bash
bash -euc '
  for service in backend api-gateway auth-service corporate-service bill-service currency-service discovery-server notification-service frontend; do
    docker compose build "$service"
  done
'
```

Bu döngü bir build başarısız olursa durur. Dokuz build de tamamlandıktan sonra:

```bash
docker compose up -d --no-build
```

Keycloak, veritabanları ve Java uygulamalarının hazır olması zaman alır. Sabit bir
başlama süresi garanti edilmez; `docker compose ps` ve gerektiğinde loglarla izleyin.
`healthy` işareti bütün kullanıcı işlemlerinin başarılı olduğunun kanıtı değildir.

Boş kurulumda otomatik hazırlananlar:

- Keycloak `bank-realm`, istemciler, roller, scope ve token mapper ayarları.
- Keycloak kimliğiyle eşleşen ilk uygulama yöneticisi.
- Grafana'da Prometheus veri kaynağı, Node Exporter ve container dashboard'ları.

Keycloak panel yöneticisi ile uygulamadaki banka yöneticisi farklı hesaplardır.
Uygulama yöneticisinin kullanıcı adı/TCKN'si `BANK_BOOTSTRAP_ADMIN_IDENTITY_NUMBER`,
parolası `BANK_BOOTSTRAP_ADMIN_PASSWORD` alanındadır. Örnekteki hesap yerel demo
içindir. Grafana için `GRAFANA_USER/GRAFANA_PASS`, Keycloak paneli için
`KC_ADMIN_USER/KC_ADMIN_PASS` kullanılır.

## 5. Erişim ve kısa kabul

| Adres | Ekran / amaç |
| --- | --- |
| `https://app.bank.local` | Uygulama |
| `https://auth.bank.local/admin/` | Keycloak yönetimi |
| `https://monitor.bank.local` | Grafana |
| `https://eureka.bank.local` | Servis kayıtları |
| `https://docs.bank.local/swagger-ui.html` | Swagger API dokümantasyonu |
| `https://api.bank.local` | API Gateway; ana sayfasında arayüz bulunması beklenmez |

İlk açılışta demo admin ile giriş yapın; ardından yeni müşteri kaydı, admin onayı,
hesap açma ve küçük bir demo transferi deneyin. Grafana'da `Bank-Demo (Compose)`
klasöründeki panellere bakın. Container panelinde host ve konteyner seçimi vardır.

Grafana'nın dosyadan gelen dashboard'ları için kaynak `grafana/` dizinidir.
Kalıcı özel değişiklikler için panelin bir kopyasını oluşturun veya kaynak dosyasını
düzenleyin; provisioning ile yönetilen paneli UI'da değiştirip kalıcı saymayın.

Nginx'in 80/443 portları host arayüzlerinde yayımlanır. DB, RabbitMQ, Redis,
Elasticsearch ve Kibana'nın host portları 127.0.0.1'e bağlıdır. Eureka ve API
dokümantasyonunu yalnız yerel/güvenilir demo ağına açın.

Kibana ve RabbitMQ yönetimine aynı hostta sırasıyla `http://127.0.0.1:5601` ve
`http://127.0.0.1:15672` ile gidilebilir. Ayrı VM'den erişirken aşağıdaki SSH
komutundaki kullanıcı ve VM IP'sini kendi bilgilerinizle değiştirin:

```bash
ssh -N -L 5601:127.0.0.1:5601 -L 15672:127.0.0.1:15672 kullanici@VM_IP
```

Tünel açıkken bu iki localhost adresini tarayıcı bilgisayarında açın. Kibana Data
View için `bank-logs-*` ve zaman alanı olarak `@timestamp` kullanılabilir.

## 6. Günlük geliştirme ve tekrar başlatma

Var olan ortamı başlatmak:

```bash
docker compose up -d
```

Frontend değişikliğini build edip çalışan sürüme geçirmek:

```bash
docker compose up -d --build --no-deps frontend
```

Diğer değişen servisler aynı komuta servis adıyla eklenebilir. `--no-deps`
örneği, bağımlılıkların zaten çalıştığı günlük geliştirme içindir.

Kaynakta `.env` değişirse yalnız `restart` yeterli değildir; ilgili container
`docker compose up -d` ile yeniden oluşturulmalıdır. `NEXT_PUBLIC_*` değişikliğinde
frontend build'i de yenilenir.

Kurulu Keycloak realm'i başlangıç importuyla yeniden yazılmaz. `.env` içindeki
client secret'ı değiştirmek Keycloak'taki secret'ı otomatik değiştirmez. Benzer
şekilde DB/Grafana'nın mevcut veri alanındaki parolaları env düzenleyerek
değiştirdiğinizi varsaymayın. Bu değişiklikler ilgili sistemde ayrıca yönetilir.
Mevcut kurulumda ilk adminin parolası her restart'ta sıfırlanmaz.

## 7. Sorun inceleme ve veri

```bash
docker compose ps
docker compose logs --tail=100 frontend api-gateway auth-service
df -h /
docker system df
```

502/503 için önce hangi servis hazır değil veya hangi çağrı başarısız bakın.
Container'ın çalışması, Keycloak girişinin ya da veritabanı işleminin çalıştığı
anlamına gelmez. Logları paylaşmadan önce token, parola ve kişisel veri içermediğini
kontrol edin.

Elasticsearch disk doluluğunda shard yerleştirmeyi veya yazmayı engelleyebilir.
%90 high watermark ile flood-stage yazma kilidi farklı eşiklerdir. Tek node'da
primary shard'lar çalışırken ayrılmamış replica nedeniyle sarı durum görülebilir.
Önce disk ve cluster durumunu inceleyin; volume silmek rutin disk temizliği değildir.

Durdurma:

```bash
docker compose stop
```

`docker compose down` container'ları kaldırır ve named volume'ları korur.
PostgreSQL, Grafana ve Prometheus named volume kullanır. Elasticsearch bu Compose'ta
kalıcı volume kullanmaz; container yeniden oluşturulunca log indexleri kaybolabilir.
RabbitMQ/Redis için bu kurulum yedeklenmiş kalıcılık garantisi vermez.

`docker compose down -v` demo veritabanlarını ve diğer named volume verilerini
siler. Bu bir sıfırlama işlemidir. Image, build cache ve uygulama verisi ayrı
şeylerdir; volume prune veri yedeğinin yerine geçmez.

Başka makineye boş kurulumda kaynak kod + yeni `.env` + sertifika/hosts hazırlığı
yeterli başlangıç girdileridir. Mevcut verileri de taşımak istiyorsanız aynı secret
değerlerini güvenli taşıyıp veritabanı ve volume'lar için ayrıca tutarlı yedek/geri
yükleme hazırlayın. Git clone veritabanı verilerini taşımaz.

Kaynaklar: [Compose başlatma seçenekleri](https://docs.docker.com/reference/cli/docker/compose/up/),
[mkcert kullanımı ve CA güveni](https://github.com/FiloSottile/mkcert).
