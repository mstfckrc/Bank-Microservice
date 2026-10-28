# Bank-Demo Core

Java 21 ve Spring Boot 4 ile hesap, bakiye ve işlem kayıtlarını yönetir.
Müşteri kimliği Auth servisinin, şirket/çalışan bilgisi Corporate servisinin,
fatura talimatları Bill servisinin sorumluluğundadır.

Sistemi çalıştırmak için [kök README](../README.md) ve
[yerel kurulum rehberini](../docs/yerel-kurulum.md) kullanın.

## Sorumluluklar

- Hesap açma ve pasife alma.
- Para yatırma, hesaplar arası transfer ve işlem onayları.
- Corporate servisinin maaş ödeme istekleri.
- Bill servisinin fatura tahsilat istekleri.
- Yetkili yönetici için hesap ve işlem sorguları.

Core kendi PostgreSQL veritabanını kullanır. Para hareketlerinin yerel transaction
sınırları bu modüldedir; diğer servislerin veritabanlarını doğrudan yönetmez.

## İletişim ve güvenlik

Korunan kullanıcı API'leri Keycloak JWT'sini ve işlem yetkisini doğrular.
Servisler arası korunan uçlar client credentials ile alınan token ve gerekli
scope'larla sınırlandırılır. Core, müşteri erişim durumunu Auth'tan ve kur bilgisini
Currency servisinden alır; bildirimleri RabbitMQ'ya yayımlar.

[Servis iletişimi diyagramı](../docs/mimari/servis-iletisimi.md) ve
[genel sistem görünümü](../docs/mimari/genel-sistem.md).

## Yapılandırma ve API dokümantasyonu

`src/main/resources/application.yml` ortam değişkenlerini okur. Yerel Compose,
repository kökündeki `.env` ayarlarını bu değişkenlere eşler.

Swagger arayüzü `https://docs.bank.local/swagger-ui.html` adresinde Gateway
üzerinden sunulur. Core container'ının `8080` portu yerel Compose'ta doğrudan
dışarı yayımlanmaz. Şema hazırlığında Hibernate `ddl-auto=update` kullanılır.
