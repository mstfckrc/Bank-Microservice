<div align="center">
  <h1>🏦 Banka Yönetim Sistemi API (Backend)</h1>
  <p><strong>Spring Boot 3 ve Spring Security ile geliştirilmiş; güvenli, ölçeklenebilir ve kurumsal düzeyde bir bankacılık backend projesi.</strong></p>
</div>

---

## 📖 Genel Bakış

**Bank-Demo-Backend**, kurumsal mimari standartlarıyla tasarlanmış kapsamlı bir banka yönetim sistemi API'sidir. Hem bireysel bankacılık işlemlerini hem de geniş kapsamlı kurumsal/yönetici yeteneklerini yönetmek için sağlam bir temel sunar.

**Java 21** ve **Spring Boot 3** üzerinde inşa edilen sistem, durumsallıktan bağımsız (stateless), güvenli kimlik doğrulama işlemleri ve Rol Tabanlı Erişim Kontrolü (RBAC) için **JWT (JSON Web Tokens)** kullanır.

---

## ✨ Öne Çıkan Özellikler

- **🔐 Güçlü Kimlik Doğrulama ve Rol Tabanlı Erişim (RBAC):** 
  - `USER`, `CORPORATE_MANAGER` ve `ADMIN` rolleriyle ayrıştırılmış JWT tabanlı güvenlik.
  - Kullanıcıların yalnızca yetkili oldukları verilere erişebilmesini sağlayan sıkı uç nokta (endpoint) koruması.
- **💰 Hassas Finansal İşlemler:** 
  - Kusursuz ve kayıpsız para hesaplamaları için `BigDecimal` kullanımı.
  - Dış servislerle entegre, anlık döviz çeviri işlemleri (Örn: TRY - USD/EUR).
- **🛡️ Kurumsal Maaş ve Personel Yönetimi:**
  - Kurumsal yöneticilerin personel işe alabileceği, maaşları yönetebileceği ve güvenli toplu maaş ödemeleri yapabileceği özel modül.
- **🧾 Otomatik Fatura Ödemeleri:**
  - Kullanıcıların elektrik, su, doğalgaz ve internet gibi faturaları için otomatik ödeme talimatı verebilmesi.
  - Aynı faturanın iki kez ödenmesini engelleyen sistem ve anlık bakiye kontrolü.
- **🚨 Merkezi Hata Yönetimi:** 
  - `@ControllerAdvice` kullanılarak Global Exception Handling (Küresel Hata Yakalama) mekanizması.
  - İstemci tarafında kolay entegrasyon için standartlaştırılmış JSON hata yanıtları.
- **🧹 Veri Bütünlüğü:** 
  - Bir kullanıcı silindiğinde ona bağlı tüm hesapların ve işlemlerin güvenle silinmesini sağlayan JPA Cascade (Orphan Removal) operasyonları.

---

## 🛠️ Teknoloji Yığını

| Kategori | Teknoloji |
| :--- | :--- |
| **Çekirdek Çerçeve (Framework)** | Java 21, Spring Boot 3 |
| **Güvenlik** | Spring Security, JWT (jjwt) |
| **Veritabanı & ORM** | PostgreSQL, Spring Data JPA, Hibernate |
| **Doğrulama (Validation)** | Spring Boot Validation |
| **Yardımcı Araçlar** | Lombok |
| **Dokümantasyon** | Swagger / OpenAPI 3 |
| **Loglama** | Logback / SLF4J |

---

## 📂 Çekirdek Mimari

Uygulama, temiz DTO izolasyonuna sahip **Controller-Service-Repository** (Katmanlı Mimari) desenini harfiyen takip eder:

- **Varlıklar (Entities):** `AppUser`, `RetailCustomer`, `Company`, `Account`, `Transaction`, `BillPaymentInstruction`, `CompanyEmployee`.
- **DTO'lar (Data Transfer Objects):** Veritabanı katmanını API sınırından ayıran Request/Response nesneleri.
- **Servis Yüzleri (Interfaces) & Gerçeklemeler (Impl):** İş mantıklarını (business logic) kendi interface'lerinden uygulayan sağlam servis yapısı.

---

## 🚀 Başlarken

### Gereksinimler
- JDK 21+
- Maven 3.8+
- PostgreSQL 15+

### Kurulum

1. **Projeyi klonlayın.**
2. **Veritabanı Ayarları:** 
   `src/main/resources/application.properties` dosyasını kendi PostgreSQL bilgilerinizle güncelleyin:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/bank_db
   spring.datasource.username=kullanici_adiniz
   spring.datasource.password=sifreniz
   ```
3. **Projeyi Derleyin:**
   ```bash
   mvn clean install
   ```
4. **Uygulamayı Çalıştırın:**
   ```bash
   mvn spring-boot:run
   ```

---

## 📚 API Dokümantasyonu

Uygulama çalıştıktan sonra, otomatik olarak oluşturulan Swagger arayüzü üzerinden uç noktaları (endpoints) etkileşimli olarak inceleyebilir ve test edebilirsiniz.

👉 **Swagger UI Yolu:** `http://localhost:8080/swagger-ui.html`

### Temel Uç Noktalar (Endpoints)
- **Kimlik (Auth):** `/api/v1/auth/login`, `/api/v1/auth/register`
- **Hesaplar:** `/api/v1/accounts`
- **İşlemler (Para Transferleri):** `/api/v1/transactions/transfer`, `/api/v1/transactions/deposit`
- **Faturalar:** `/api/v1/bills/instructions`
- **Yönetici (Admin):** `/api/v1/admin/customers`, `/api/v1/admin/accounts`
- **Kurumsal:** `/api/v1/companies/employees`

---

<div align="center">
  <i>Mustafa tarafından ❤️ ile geliştirilmiştir.</i>
</div>