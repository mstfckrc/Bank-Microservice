<div align="center">
  <h1>💻 Banka Yönetim Sistemi (Frontend)</h1>
  <p><strong>Next.js App Router ve Tailwind CSS ile inşa edilmiş; modern, duyarlı (responsive) ve tip güvenli (type-safe) bankacılık paneli.</strong></p>
</div>

---

## 📖 Genel Bakış

**Bank-Demo-Frontend**, güçlü banka yönetim sistemimizin kullanıcı arayüzü (UI) katmanıdır. Bireysel Müşteriler, Kurumsal Yöneticiler ve Sistem Yöneticileri için birbirinden bağımsız ve güvenli yönetim panelleri sunar.

Modern **App Router** mimarisini kullanarak **Next.js 14+** üzerinde inşa edilen bu proje; katı yerleşim (layout) gruplamaları, Zustand ile merkezi durum yönetimi (state management) ve yüksek oranda optimize edilmiş UI bileşenleriyle donatılmıştır.

---

## ✨ Öne Çıkan Özellikler

- **🔒 Akıllı Yönlendirme & Güvenlik Yazılımı (Middleware):** 
  - Hassas rotaları koruyan dinamik `middleware.ts` yapısı.
  - Kullanıcıları, sahip oldukları JWT rolüne (`RETAIL_CUSTOMER`, `CORPORATE_MANAGER`, `ADMIN`) göre otomatik olarak kendi panellerine yönlendirir.
- **📊 Özel Tahsis Edilmiş Paneller (Dashboards):**
  - **Kullanıcı (User) Paneli:** Bireylerin hesaplarını yönetmesi, para transferi yapması, hesaba para yatırması ve otomatik fatura ödeme talimatları verebilmesi içindir.
  - **Şirket (Company) Paneli:** Kurumsal müşterilerin iş gücünü yönetmesi, yeni personel işe alması ve toplu olarak saniyeler içinde maaş dağıtımı yapabilmesi içindir.
  - **Yönetici (Admin) Paneli:** Banka personelinin tüm kullanıcıları izleyebildiği, işlemleri denetleyebildiği ve global sistem hesaplarını yönetebildiği God-Mode paneli.
- **⚡ İyimser UI (Optimistic Updates) & Canlı Modallar:**
  - Veri çekme işlemleri için oluşturulmuş dinamik React Hook'ları (`useAccounts`, `useCompanyEmployees`, vb.).
  - Sayfa yenilemeye gerek kalmadan değişiklikleri anında yansıtan, kendi durumlarını (state) yöneten etkileşimli modallar.
- **🎨 Modern Estetik:**
  - **Tailwind CSS** ve özelleştirilmiş modern UI bileşenleri (Shadcn UI alternatifleri) ile şekillendirilmiş arayüz.
  - Birbirinden ayrılmış temiz gezinme çubukları (navbar), yan menüler (sidebar) ve durum etiketlerinden (status badges) oluşan yalın yerleşim.

---

## 🛠️ Teknoloji Yığını

| Kategori | Teknoloji |
| :--- | :--- |
| **Çerçeve (Framework)** | Next.js 14+ (App Router) |
| **Dil** | TypeScript |
| **Durum Yönetimi (State)** | Zustand (`useAuthStore`) |
| **API İstemcisi** | Axios |
| **Stil/Tasarım** | Tailwind CSS |
| **İkonlar & Yazı Tipleri**| Lucide React, Geist Font |

---

## 📂 Çekirdek Mimari

- **`app/`**: Next.js rota yapısını barındırır. Ortak yerleşim planlarını (layouts) uygulamak için `(auth)` ve `(dashboard)` gibi mantıksal rota gruplarını kullanır.
- **`components/`**: Bağlama göre gruplandırılmış (`admin/`, `company/`, `dashboard/`, `shared/`, `ui/`) modüler, yeniden kullanılabilir arayüz öğeleri.
- **`hooks/`**: İş mantığını ve API koordinasyonunu sayfalardan soyutlayan Özel React Hook'ları (Örn: `useBills.ts`, `useCustomers.ts`).
- **`services/`**: Backend uç noktalarını (endpoints) hedefleyen Axios tabanlı API sargıları (wrappers).
- **`types/`**: Kusursuz tip güvenliği sağlamak adına Backend'in Veri Transfer Nesneleriyle (DTO'lar) birebir eşleşen kapsamlı TypeScript arayüzleri (interfaces).
- **`store/`**: Zustand ile güçlendirilmiş global hafıza (state management).

---

## 🚀 Başlarken

### Gereksinimler
- Node.js 18.17+
- npm, yarn, veya bun

### Kurulum

1. **Projeyi klonlayın.**
2. **Bağımlılıkları Yükleyin:**
   ```bash
   npm install
   # veya
   yarn install
   ```
3. **Ortam (Environment) Ayarları:**
   Kök dizinde bir `.env.local` dosyası oluşturun ve Backend API adresinizi girin:
   ```env
   NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
   ```
4. **Geliştirme Sunucusunu Başlatın:**
   ```bash
   npm run dev
   ```

### 🌍 Uygulamaya Erişim

Tarayıcınızda [http://localhost:3000](http://localhost:3000) adresine gidin. 
- Bireysel bir müşteri hesabı açarak sistemi deneyebilir veya backend tarafında yetkilendirilmiş bir admin hesabı ile giriş yapıp tüm özellikleri test edebilirsiniz.

---

<div align="center">
  <i>Yeni nesil bankacılık deneyimi için özenle kodlanmıştır.</i>
</div>
