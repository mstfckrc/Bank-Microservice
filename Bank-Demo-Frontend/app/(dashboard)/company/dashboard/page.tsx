"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/useAuthStore";
import { useCompanyEmployees } from "@/hooks/useCompanyEmployees";
import { useAccounts } from "@/hooks/useAccounts";
import { companyService } from "@/services/company.service"; // 🚀 YENİ SERVİS

// Modallar
import EmployeeModal from "@/components/company/modals/EmployeeModal";
import CorporateVaultModal from "@/components/company/modals/CorporateVaultModal";
import CorporateDepositModal from "@/components/company/modals/CorporateDepositModal";
import CorporateHistoryModal from "@/components/company/modals/CorporateHistoryModal";
import CorporateTransferModal from "@/components/company/modals/CorporateTransferModal";
import CorporateCloseAccountModal from "@/components/company/modals/CorporateCloseAccountModal";
import CorporatePaySalariesModal from "@/components/company/modals/CorporatePaySalariesModal";
import CorporateAutoPaymentModal from "@/components/company/modals/CorporateAutoPaymentModal"; // 🚀 YENİ MODAL

// Tipler ve İkonlar
import {
  CompanyEmployeeResponse,
  HireEmployeeRequest,
  UpdateEmployeeRequest,
  AutoPaymentSettingsRequest, // 🚀 YENİ TİP
} from "@/types";
import { Trash2, Globe, Settings, CalendarClock } from "lucide-react"; // 🚀 YENİ İKON EKLENDİ
import { toast } from "sonner";

export default function CompanyDashboardPage() {
  const router = useRouter();
  const { user } = useAuthStore();

  // Şirket Onay Durumu Kontrolleri
  const isApproved = user?.status === "APPROVED";
  const isPending = user?.status === "PENDING";
  const isRejected = user?.status === "REJECTED";

  const {
    employees,
    loading: empLoading,
    isProcessing: empProcessing,
    error: empError,
    fetchEmployees,
    removeEmployee,
    hireEmployee,
    updateEmployee,
    paySalaries,
  } = useCompanyEmployees();

  const {
    accounts,
    loading: accLoading,
    isProcessing: accProcessing,
    fetchAccounts,
    createMyAccount,
    closeAccount,
  } = useAccounts();

  // --- MODAL STATE YÖNETİMİ ---
  const [isEmployeeModalOpen, setIsEmployeeModalOpen] = useState(false);
  const [selectedEmployee, setSelectedEmployee] =
    useState<CompanyEmployeeResponse | null>(null);
  const [isVaultModalOpen, setIsVaultModalOpen] = useState(false);
  const [isSalaryModalOpen, setIsSalaryModalOpen] = useState(false);

  // 🚀 YENİ: Otomatik Ödeme Modal State'leri
  const [isAutoPaymentModalOpen, setIsAutoPaymentModalOpen] = useState(false);
  const [isAutoPaymentSaving, setIsAutoPaymentSaving] = useState(false);
  const [autoPaymentSettings, setAutoPaymentSettings] = useState<any>(null); // 🚀 YENİ: Veritabanından gelen ayarları tutacağımız state

  const [depositAccount, setDepositAccount] = useState<{
    accountNumber: string;
    iban: string;
    currency: string;
  } | null>(null);
  const [historyAccount, setHistoryAccount] = useState<{
    id: number;
    accountNumber: string;
    currency: string;
  } | null>(null);
  const [transferAccount, setTransferAccount] = useState<{
    accountNumber: string;
    iban: string;
    currency: string;
  } | null>(null);
  const [accountToClose, setAccountToClose] = useState<string | null>(null);

  useEffect(() => {
    fetchEmployees();
    fetchAccounts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // --- İŞLEM YÖNETİCİLERİ ---
  const handleDelete = async (identityNumber: string, fullName: string) => {
    if (
      window.confirm(
        `${fullName} isimli personeli işten çıkarmak (sistemden silmek) istediğinize emin misiniz?`,
      )
    ) {
      await removeEmployee(identityNumber);
    }
  };

  const handleAddNewEmployee = () => {
    setSelectedEmployee(null);
    setIsEmployeeModalOpen(true);
  };

  const handleEditEmployee = (employee: CompanyEmployeeResponse) => {
    setSelectedEmployee(employee);
    setIsEmployeeModalOpen(true);
  };

  const handleSaveEmployee = async (
    data: HireEmployeeRequest | UpdateEmployeeRequest,
    isEditMode: boolean,
  ) => {
    if (isEditMode && selectedEmployee) {
      return await updateEmployee(
        selectedEmployee.identityNumber,
        data as UpdateEmployeeRequest,
      );
    } else {
      return await hireEmployee(data as HireEmployeeRequest);
    }
  };

  const handleCloseAccountConfirm = async () => {
    if (accountToClose) {
      const success = await closeAccount(accountToClose);
      if (success) {
        setAccountToClose(null);
      }
    }
  };

  const handlePaySalaries = async (senderIban: string) => {
    const results = await paySalaries(senderIban);
    if (results) {
      toast.success("Maaşlar Başarıyla Dağıtıldı!", {
        description: `${results.length} personelin hesabına transfer talimatı gönderildi.`,
      });
      setIsSalaryModalOpen(false);
      fetchAccounts();
    }
  };

  // 🚀 YENİ İŞLEM: Otomatik Ödeme Ayarlarını Kaydetme
  const handleSaveAutoPayment = async (data: AutoPaymentSettingsRequest) => {
    setIsAutoPaymentSaving(true);
    try {
      const response = await companyService.updateAutoPaymentSettings(data);
      toast.success("Ayarlar Kaydedildi!", { description: response.message });
      return true;
    } catch (err: any) {
      toast.error("Hata", {
        description: err.response?.data?.message || "Ayarlar kaydedilemedi.",
      });
      return false;
    } finally {
      setIsAutoPaymentSaving(false);
    }
  };

  // 🚀 YENİ: Modalı açmadan önce veritabanından mevcut ayarları çek
  const handleOpenAutoPaymentModal = async () => {
    try {
      // Not: Eğer companyService içinde getAutoPaymentSettings yoksa, backend'e GET atan ufak bir metot eklemelisin.
      const currentSettings = await companyService.getAutoPaymentSettings();
      setAutoPaymentSettings(currentSettings);
    } catch (err) {
      console.log("Mevcut ayarlar çekilemedi veya henüz ayar yapılmamış.");
      setAutoPaymentSettings(null); // Hata verirse (veya ayar yoksa) boş açsın
    }
    setIsAutoPaymentModalOpen(true);
  };

  // --- ARAYÜZ (UI) RENDER ALANI ---

  if (empLoading && employees.length === 0) {
    return (
      <div className="p-8 text-center text-gray-500 animate-pulse">
        Kurumsal veriler yükleniyor...
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4 md:p-8">
      {/* ÜST BİLGİ PANELİ */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mb-6 flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">
            Kurumsal Yönetim Paneli
          </h1>
          <p className="text-gray-500 mt-1">
            Hoş geldiniz,{" "}
            <span className="font-semibold text-blue-600">
              {user?.profileName}
            </span>
          </p>
        </div>
        <div className="text-right">
          <p className="text-sm text-gray-500">Toplam Personel</p>
          <p className="text-3xl font-bold text-gray-800">{employees.length}</p>
        </div>
      </div>

      {/* ONAY DURUMU BANNER'LARI */}
      {!isApproved && (
        <div
          className={`mb-6 p-5 rounded-xl border-l-4 shadow-sm flex items-start gap-4 ${
            isRejected
              ? "bg-red-50 border-red-500 text-red-800"
              : "bg-yellow-50 border-yellow-500 text-yellow-800"
          }`}
        >
          <div className="mt-0.5">
            {isRejected ? (
              <svg
                className="w-6 h-6 text-red-500"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                />
              </svg>
            ) : (
              <svg
                className="w-6 h-6 text-yellow-500"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            )}
          </div>
          <div>
            <h3 className="font-bold text-lg mb-1">
              {isRejected
                ? "Kurumsal Hesabınız Reddedildi!"
                : "Kurumsal Hesabınız Onay Bekliyor"}
            </h3>
            <p className="opacity-90 text-sm">
              {isRejected
                ? "Hesap başvurunuz güvenlik politikalarımız gereği reddedilmiştir. Finansal işlem yapamaz ve personel ekleyemezsiniz. Lütfen şubenizle iletişime geçin."
                : "Hesabınız şu anda MASAK ve Banka yetkililerimiz tarafından incelenmektedir. Onaylanana kadar kasa açamaz veya işlem yapamazsınız."}
            </p>
          </div>
        </div>
      )}

      {/* HIZLI İŞLEMLER MENÜSÜ */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <button
          onClick={() => router.push("/currencies")}
          className="bg-white p-4 rounded-xl border border-gray-100 shadow-sm hover:shadow-md hover:border-purple-200 transition-all flex flex-col items-center justify-center gap-3 group"
        >
          <div className="bg-purple-50 p-3 rounded-full text-purple-600 group-hover:bg-purple-100 transition-colors">
            <Globe className="w-6 h-6 group-hover:scale-110 transition-transform" />
          </div>
          <span className="text-sm font-bold text-gray-700">Piyasa Ekranı</span>
        </button>

        <button
          onClick={() => router.push("/company/settings")}
          className="bg-white p-4 rounded-xl border border-gray-100 shadow-sm hover:shadow-md hover:border-orange-200 transition-all flex flex-col items-center justify-center gap-3 group"
        >
          <div className="bg-orange-50 p-3 rounded-full text-orange-600 group-hover:bg-orange-100 transition-colors">
            <Settings className="w-6 h-6 group-hover:rotate-45 transition-transform duration-300" />
          </div>
          <span className="text-sm font-bold text-gray-700">
            Kurumsal Ayarlar
          </span>
        </button>
      </div>

      {/* KURUMSAL KASALAR (HESAPLAR) BÖLÜMÜ */}
      <div className="mb-8">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-lg font-semibold text-gray-700">
            Kurumsal Hesaplar (Kasa)
          </h2>

          <button
            onClick={() => setIsVaultModalOpen(true)}
            disabled={!isApproved || accProcessing}
            className="text-sm bg-indigo-50 text-indigo-700 hover:bg-indigo-100 hover:text-indigo-800 px-4 py-2 rounded-lg font-semibold transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <svg
              className="w-4 h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 4v16m8-8H4"
              />
            </svg>
            Yeni Kasa Aç
          </button>
        </div>

        {accLoading ? (
          <div className="text-sm text-gray-500 animate-pulse">
            Kasalar kontrol ediliyor...
          </div>
        ) : accounts.length === 0 ? (
          <div className="bg-yellow-50 border-l-4 border-yellow-500 p-4 rounded text-yellow-700 text-sm shadow-sm flex items-center justify-between">
            <span>
              Şirketinize ait aktif bir banka hesabı bulunmuyor. Maaş ödemeleri
              ve transferler için hemen bir kasa oluşturun.
            </span>
          </div>
        ) : (
          <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
            {accounts.map((acc) => {
              const isAccountActive =
                acc.isActive !== false && (acc as any).active !== false;

              return (
                <div
                  key={acc.id}
                  className="bg-linear-to-br from-slate-800 to-slate-900 rounded-xl p-5 shadow-lg text-white relative overflow-hidden border border-slate-700 flex flex-col justify-between group"
                >
                  <div className="absolute top-0 right-0 p-4 opacity-10">
                    <svg
                      className="w-16 h-16"
                      fill="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.31-8.86c-1.77-.45-2.34-.94-2.34-1.67 0-.84.79-1.43 2.1-1.43 1.38 0 1.9.66 1.94 1.64h1.71c-.05-1.34-.87-2.57-2.49-2.97V5H10.9v1.69c-1.51.32-2.72 1.3-2.72 2.81 0 1.79 1.49 2.69 3.66 3.21 1.95.46 2.34 1.15 2.34 1.87 0 .53-.39 1.64-2.25 1.64-1.74 0-2.26-.97-2.32-1.81h-1.7c.07 1.65 1.12 2.88 2.87 3.27V19h2.08v-1.63c1.55-.33 2.88-1.25 2.88-3 0-2.21-1.88-2.92-3.43-3.23z" />
                    </svg>
                  </div>

                  {isAccountActive ? (
                    <button
                      onClick={() => setAccountToClose(acc.accountNumber)}
                      disabled={!isApproved}
                      className="absolute top-4 right-4 text-slate-400 hover:text-red-400 opacity-0 group-hover:opacity-100 transition-all duration-300 disabled:opacity-0"
                      title="Kasayı Kapat"
                    >
                      <Trash2 className="w-5 h-5" />
                    </button>
                  ) : (
                    <span className="absolute top-4 right-4 bg-red-500 text-white text-xs px-2 py-1 rounded font-bold">
                      KAPALI
                    </span>
                  )}

                  <div>
                    <p className="text-slate-400 text-xs font-medium uppercase tracking-wider mb-1">
                      Kasa Bakiyesi
                    </p>
                    <h3 className="text-2xl font-bold mb-4">
                      {acc.balance.toLocaleString("tr-TR")}{" "}
                      <span className="text-sm font-normal">
                        {acc.currency}
                      </span>
                    </h3>
                    <p className="text-slate-300 font-mono text-sm">
                      {acc.iban}
                    </p>
                  </div>

                  {isAccountActive && (
                    <div className="mt-5 grid grid-cols-3 gap-2">
                      <button
                        onClick={() =>
                          setDepositAccount({
                            accountNumber: acc.accountNumber,
                            iban: acc.iban,
                            currency: acc.currency,
                          })
                        }
                        disabled={!isApproved}
                        className="bg-emerald-500/20 hover:bg-emerald-500/40 text-emerald-100 py-2 px-1 rounded-lg text-xs font-semibold transition-colors flex items-center justify-center gap-1 border border-emerald-400/20 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <svg
                          className="w-3.5 h-3.5 hidden sm:block"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M12 6v6m0 0v6m0-6h6m-6 0H6"
                          />
                        </svg>
                        Yatır
                      </button>
                      <button
                        onClick={() =>
                          setTransferAccount({
                            accountNumber: acc.accountNumber,
                            iban: acc.iban,
                            currency: acc.currency,
                          })
                        }
                        disabled={!isApproved}
                        className="bg-indigo-500/20 hover:bg-indigo-500/40 text-indigo-100 py-2 px-1 rounded-lg text-xs font-semibold transition-colors flex items-center justify-center gap-1 border border-indigo-400/20 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <svg
                          className="w-3.5 h-3.5 hidden sm:block"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4"
                          />
                        </svg>
                        Transfer
                      </button>
                      <button
                        onClick={() =>
                          setHistoryAccount({
                            id: acc.id,
                            accountNumber: acc.accountNumber,
                            currency: acc.currency,
                          })
                        }
                        disabled={!isApproved}
                        className="bg-blue-500/20 hover:bg-blue-500/40 text-blue-100 py-2 px-1 rounded-lg text-xs font-semibold transition-colors flex items-center justify-center gap-1 border border-blue-400/20 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <svg
                          className="w-3.5 h-3.5 hidden sm:block"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                          />
                        </svg>
                        Geçmiş
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* HATA MESAJI */}
      {empError && (
        <div className="bg-red-50 border-l-4 border-red-500 text-red-700 p-4 mb-6 rounded shadow-sm">
          {empError}
        </div>
      )}

      {/* TABLO BÖLÜMÜ */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="p-6 border-b border-gray-100 flex justify-between items-center bg-gray-50 flex-wrap gap-4">
          <h2 className="text-lg font-semibold text-gray-700">
            Maaş Bordrosu & Çalışanlar
          </h2>

          {/* 🚀 ÜÇ BUTON YAN YANA GELDİ */}
          <div className="flex gap-2 flex-wrap">
            <button
              onClick={handleOpenAutoPaymentModal}
              disabled={!isApproved}
              className="bg-blue-100 text-blue-700 hover:bg-blue-200 px-4 py-2 rounded-lg text-sm font-bold transition-colors shadow-sm disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            >
              <CalendarClock className="w-4 h-4" />
              Otomatik Talimat
            </button>

            <button
              onClick={() => setIsSalaryModalOpen(true)}
              disabled={!isApproved || employees.length === 0}
              className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors shadow-sm disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            >
              <svg
                className="w-4 h-4"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              Maaşları Dağıt
            </button>

            <button
              onClick={handleAddNewEmployee}
              disabled={!isApproved || empProcessing}
              className="bg-slate-800 hover:bg-slate-900 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
            >
              + Yeni Personel
            </button>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50 text-gray-500 text-xs uppercase tracking-wider">
                <th className="p-4 border-b">TC Kimlik</th>
                <th className="p-4 border-b">Ad Soyad</th>
                <th className="p-4 border-b">Maaş IBAN</th>
                <th className="p-4 border-b text-right">Maaş (TL)</th>
                <th className="p-4 border-b text-center">İşlemler</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {employees.length === 0 ? (
                <tr>
                  <td colSpan={5} className="p-8 text-center text-gray-500">
                    Henüz şirketinize kayıtlı bir personel bulunmuyor.
                  </td>
                </tr>
              ) : (
                employees.map((emp) => (
                  <tr
                    key={emp.id}
                    className="hover:bg-gray-50 transition-colors"
                  >
                    <td className="p-4 text-sm text-gray-600">
                      {emp.identityNumber}
                    </td>
                    <td className="p-4 font-medium text-gray-800">
                      {emp.firstName} {emp.lastName}
                    </td>
                    <td className="p-4 text-sm font-mono text-gray-500">
                      {emp.salaryIban}
                    </td>
                    <td className="p-4 text-right font-semibold text-gray-800">
                      {emp.salaryAmount.toLocaleString("tr-TR")} ₺
                    </td>
                    <td className="p-4 text-center">
                      <button
                        onClick={() => handleEditEmployee(emp)}
                        disabled={!isApproved || empProcessing}
                        className="text-blue-500 hover:text-blue-700 font-medium text-sm mr-4 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        Düzenle
                      </button>
                      <button
                        onClick={() =>
                          handleDelete(
                            emp.identityNumber,
                            `${emp.firstName} ${emp.lastName}`,
                          )
                        }
                        disabled={!isApproved || empProcessing}
                        className="text-red-500 hover:text-red-700 font-medium text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        Çıkar
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* --- MODALLAR --- */}
      <EmployeeModal
        isOpen={isEmployeeModalOpen}
        onClose={() => setIsEmployeeModalOpen(false)}
        selectedEmployee={selectedEmployee}
        onSave={handleSaveEmployee}
      />
      <CorporateVaultModal
        isOpen={isVaultModalOpen}
        onClose={() => setIsVaultModalOpen(false)}
        onCreate={createMyAccount}
        isProcessing={accProcessing}
      />
      <CorporateDepositModal
        isOpen={depositAccount !== null}
        onClose={() => setDepositAccount(null)}
        accountNumber={depositAccount?.accountNumber || ""}
        iban={depositAccount?.iban || ""}
        currency={depositAccount?.currency || "TRY"}
        onSuccess={() => fetchAccounts()}
      />
      <CorporateHistoryModal
        isOpen={historyAccount !== null}
        onClose={() => setHistoryAccount(null)}
        accountId={historyAccount?.id || 0}
        accountNumber={historyAccount?.accountNumber || ""}
        currency={historyAccount?.currency || "TRY"}
      />
      <CorporateTransferModal
        isOpen={transferAccount !== null}
        onClose={() => setTransferAccount(null)}
        senderIban={transferAccount?.iban || ""}
        accountNumber={transferAccount?.accountNumber || ""}
        currency={transferAccount?.currency || "TRY"}
        onSuccess={() => fetchAccounts()}
      />
      <CorporateCloseAccountModal
        accountToClose={accountToClose}
        isClosing={accProcessing}
        onCancel={() => setAccountToClose(null)}
        onConfirm={handleCloseAccountConfirm}
      />
      <CorporatePaySalariesModal
        isOpen={isSalaryModalOpen}
        onClose={() => setIsSalaryModalOpen(false)}
        accounts={accounts}
        employees={employees}
        onConfirm={handlePaySalaries}
        isProcessing={empProcessing}
      />
      {/* 🚀 YENİ: OTOMATİK ÖDEME MODALI EKLENDİ */}
      <CorporateAutoPaymentModal
        isOpen={isAutoPaymentModalOpen}
        onClose={() => setIsAutoPaymentModalOpen(false)}
        accounts={accounts}
        onConfirm={handleSaveAutoPayment}
        isProcessing={isAutoPaymentSaving}
        initialData={autoPaymentSettings} // 🚀 İŞTE BEYİN BURASI!
      />
    </div>
  );
}
