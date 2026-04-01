# 🏦 Bank Microservices Architecture 
**Enterprise-Grade Scalable Banking System**

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-brightgreen.svg?logo=spring)
![Next.js](https://img.shields.io/badge/Next.js-14-black.svg?logo=next.js)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg?logo=postgresql)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-AMQP-FF6600.svg?logo=rabbitmq)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?logo=docker)
![Microservices](https://img.shields.io/badge/Architecture-Distributed-purple.svg)

**Bank**, modern bankacılık operasyonlarını yürütebilmek amacıyla Monolitik yapılardan arındırılarak bütünüyle dağıtık **Spring Cloud Mikroservis Mimarisi** standartlarına geçirilmiş kurumsal düzeyde bir finansal teknolojiler platformudur. Sistem, Müşteri Yönetimi, Finansal Çekirdek, Kurumsal Operasyonlar ve Fatura Sistemlerini birbirlerinden tam izole yapı taşları (Bounded Contexts) halinde orkestre eder.

---

## 🏗️ Sistem Mimarisi (Architecture)

Proje, birbirleriyle yüksek uyum içerisinde aynı zamanda bağımsız ölçeklenebilen **8 Farklı Mikroservis** ve **1 Frontend** istemcisinden oluşmaktadır:

1. 🚪 **API Gateway (`api-gateway` | :8081)**
   - Tüm dış isteklerin (Frontend/Mobil) tek giriş noktasıdır.
   - `AuthenticationFilter` ile kurumsal 512-bit JWT doğrulamasını gerçekleştirir. Kimliği doğrulanan isteklere `X-Identity-Number` güvenlik başlığını enjekte ederek güvenli iç hat dağıtımını sağlar.
2. 🔐 **Identity & Auth Service (`bank-auth-micro` | :8086)**
   - Giriş-Çıkış (Login/Register), Müşteri Profili ve Admin yetkilendirmelerini barındıran Kimlik ve Erişim Yönetimi (IAM) merkezidir.
3. 🏦 **Core Banking Service (`bank-demo-backend` | :8080)**
   - Sistemin finansal çekirdeğidir. Müşteri hesapları (IBAN atamaları), bakiyeler, para transferleri (EFT/Havale) ve MASAK limit kural motorlarını yönetir. Hiçbir şekilde kullanıcı bilgisi barındırmaz; yalnızca finansal verilere odaklanır.
4. 🏢 **Corporate Management Service (`bank-corporate-micro` | :8085)**
   - Şirketlere özel İK ve Maaş otomasyon servisidir. Gece yarısı tetiklenen asenkron görevlerle on binlerce kurum personeline toplu maaş ödemesi emrini Karargaha (Core Banking) iletir. Kendi veritabanına sahiptir.
5. 🧾 **Billing & Utilities Service (`bill-service` | :8084)**
   - Elektrik, su doğalgaz gibi rutin faturalandırma ve otomatik ödeme takvimlerinin yönetildiği bağımsız modüldür.
6. 💶 **Currency API Service (`currency-service` | :8083)**
   - Harici döviz borsası entegratörüdür. Gerçek zamanlı uluslararası para kurlarını alır ve çapraz döviz transferlerinde motor olarak görev yapar.
7. 📯 **Notification Service (`notification-service` | :8082)**
   - Sistemin dijital postanesidir. RabbitMQ üzerinden aldığı olayları (Events) kullanıcıya SMS, Push ve E-Posta olarak gecikmesiz ulaştıran asenkron tüketici (Consumer) katmanıdır.
8. 🌐 **Service Registry (`discovery-server` | :8761)**
   - Spring Cloud Eureka altyapısıyla servis keşif ve kayıt santralidir. Ağ dâhilindeki tüm modülleri maskeleyerek iletişimde IP bağımlılığını veya port donanımlarını ekarte eder.

---

## 🛠️ Temel Teknolojiler (Tech Stack)

### Backend (Java Ecosystem)
- **Java 17+** & **Spring Boot 3.x**
- **Spring Cloud** (Gateway, Eureka, OpenFeign)
- **Spring Security & JWT** (Stateless Auth)
- **Spring Data JPA & Hibernate** 
- **PostgreSQL** (Dedicated per Service)
- **RabbitMQ** (Message Broker & Event-Driven Architecture)
- **Logstash / ELK** (Centralized Logging - Kurulum Aşamasında)

### Frontend (User Experience)
- **Next.js 14** (App Router & SSR)
- **React.js & Tailwind CSS** / Shadcn UI
- **Zustand** (State Management)
- **Axios** (API Interceptor)

---

## 🔒 Güvenlik Senaryosu & Haberleşme

Sistem "Sıfır Güven (Zero Trust)" ve "Tek Yönlü Yetkilendirme" standartlarıyla dizayn edilmiştir:

*   **REST Sınırları (FeignClients):**
    Mikroservisler diğer servislerin veritabanına asla erişemez. İhtiyaç anında `OpenFeign` üzerinden senkron iç haberleşme kullanılarak paketler (Örn: `CompanySyncRequest`) ilgili adrese fırlatılır. İç hat görüşmelerinde `X-Internal-Identity` güvenlik köprüleri kullanılır.
*   **Ağır Yıkım (Cascading Deletes) Orkestrası:** 
    Dağıtık sistemde bütünlük (Consistency) ihlalini engellemek için, bir kullanıcının silinmesi işlemi, Auth Servisinin kontrolünde `Saga/Orchestration` benzeri bir yaklaşımla; peş peşe hesap kapatma, profil kurutma ve kimlik silme aşamaları şeklinde yürütülür.

---

## 🚀 Başlangıç ve Çalıştırma (Getting Started)

Proje `Docker Compose` üzerinde tam yapılandırılmıştır.

```bash
# 1. Projeyi sisteminize indirin
git clone https://github.com/mstfckrc/Bank-Microservice.git
cd Bank-Microservice

# 2. Arka plan servislerini, veritabanlarını ve mesaj kuyruğunu ayağa kaldırın
docker-compose up -d

# 3. Frontend klasörüne girip arayüzü başlatın
cd Bank-Demo-Frontend
npm install
npm run dev
```

* **Frontend Arayüzü:** `http://localhost:3000`
* **Eureka Dashboard:** `http://localhost:8761`
* **API Gateway Portülü:** `http://localhost:8081`

---
*Bu mimari, Sürüm 6.0 standartlarında kurumsal mikroservis izolasyon ilkelerine ve endüstri pratiklerine sadık kalınarak dökümante edilmiştir.*
