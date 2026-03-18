// --- ORTAK TİPLER ---
// 🚀 YENİ: Roller kurumsal ve bireysel olarak ayrıldı
export type Role = "ADMIN" | "RETAIL_CUSTOMER" | "CORPORATE_MANAGER";
export type Currency = string;

// Müşteri Onay Durumları
export type ApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";

// İşlem Onay Durumları (MASAK)
export type TransactionStatus = "COMPLETED" | "PENDING_APPROVAL" | "REJECTED";


// --- AUTH (GİRİŞ/KAYIT) MODELLERİ ---
export interface LoginRequest {
  identityNumber: string; // 🚀 tcNo yerine artık identityNumber (TC veya Vergi No)
  password: string;
}

export interface RegisterRequest {
  identityNumber: string; 
  password: string;
  role: Role;           // 🚀 Kayıt anında seçilen rol
  email: string;
  // --- Bireysel Alanlar ---
  firstName?: string;
  lastName?: string;
  // --- Kurumsal Alanlar ---
  companyName?: string;
  taxOffice?: string;
}

export interface AuthResponse {
  token: string;
}

// --- PROFİL MODELLERİ (Eski CustomerResponse Emekli Edildi) ---
export interface UserProfileResponse {
  identityNumber: string; 
  profileName: string;    // 🚀 Birey için "Ad Soyad", Şirket için "Şirket Ünvanı"
  email: string;
  role: Role;
  status: ApprovalStatus; 
}

export interface UpdateProfileRequest {
  profileName?: string;   // 🚀 fullName yerine
  email?: string;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}


// --- HESAP (ACCOUNT) MODELLERİ ---
export interface AccountResponse {
  id: number;
  accountNumber: string;
  iban: string;      
  balance: number;
  currency: string;
  isActive: boolean;
  ownerName: string;      // 🚀 Dinamik hesap sahibi adı
  identityNumber: string; // 🚀 Sahibinin TC/Vergi No'su
}

export interface CreateAccountRequest {
  currency: 'TRY' | 'USD' | 'EUR';
}


// --- İŞLEM (TRANSACTION) MODELLERİ ---
export type TransactionType = 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER';

export interface TransferRequest {
  senderIban: string;
  receiverIban: string;
  amount: number;
  description?: string; 
}

export interface DepositRequest {
  iban: string;
  amount: number;
}

export interface TransactionResponse {
  referenceNo: string;
  amount: number;
  convertedAmount?: number; 
  transactionType: TransactionType;
  description: string;
  transactionDate: string; 
  senderAccountId?: number | null; 
  receiverAccountId?: number | null;
  status?: TransactionStatus; 
}


// --- HATA VE DÖVİZ MODELLERİ ---
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

export interface ExchangeRateResponse {
  base: string;
  date: string;
  rates: {
    [key: string]: number; 
  };
}

// --- KURUMSAL PERSONEL YÖNETİMİ TİPLERİ ---

export interface CompanyEmployeeResponse {
  id: number;
  identityNumber: string;
  firstName: string;
  lastName: string;
  salaryIban: string;
  salaryAmount: number;
}

export interface HireEmployeeRequest {
  identityNumber: string;
  salaryIban: string;
  salaryAmount: number;
}

export interface UpdateEmployeeRequest {
  salaryIban: string;
  salaryAmount: number;
}

export interface AutoPaymentSettingsRequest {
  autoPaymentEnabled: boolean;
  paymentDay: number | null;
  defaultSalaryIban: string;
}

export interface AutoPaymentSettingsResponse {
  autoPaymentEnabled: boolean;
  paymentDay: number;
  defaultSalaryIban: string;
  message: string;
}

// --- FATURA (BILL) MODELLERİ ---
export type BillType = 'ELECTRICITY' | 'WATER' | 'INTERNET' | 'GAS';

export interface BillInstructionRequest {
  accountId: number;
  billType: BillType;
  subscriberNo: string;
  paymentDay: number;
}

export interface BillInstructionResponse {
  id: number;
  accountNumber: string;
  iban: string;
  billType: BillType;
  subscriberNo: string;
  paymentDay: number;
  lastPaymentDate: string | null;
  isActive: boolean;
}