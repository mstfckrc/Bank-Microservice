import { create } from "zustand";
import { persist, devtools } from "zustand/middleware";
import Cookies from "js-cookie";
import { Role, ApprovalStatus, UserProfileResponse } from "../types"; // Kendi yoluna göre ayarla

export interface AuthUser {
  identityNumber: string;
  profileName: string;
  email: string;
  role: Role;
  status: ApprovalStatus;
}

interface AuthState {
  user: AuthUser | null;
  token: string | null;
  isAuthenticated: boolean;

  login: (user: AuthUser, token: string) => void;
  logout: () => void;
  updateUser: (updatedFields: Partial<AuthUser>) => void;
}

export const useAuthStore = create<AuthState>()(
  devtools(
    persist(
      (set) => ({
        user: null,
        token: null,
        isAuthenticated: false,

        // 1. GİRİŞ YAPMA (LOGIN)
        login: (user, token) => {
          Cookies.set("token", token, {
            expires: 1,
            // 🚀 DÜZELTME 1: Sadece Docker/Canlı ortamda HTTPS (secure) zorunlu olsun
            secure: process.env.NODE_ENV === "production", 
            sameSite: "strict",
            // 🚀 DÜZELTME 2: Çerezin projenin her rotasında okunabilmesi için kök dizin atadık
            path: "/", 
          });
          set({ user, token, isAuthenticated: true });
        },

        // 2. ÇIKIŞ YAPMA (LOGOUT)
        logout: () => {
          // 🚀 DÜZELTME 3: Çerezi silerken de path belirtmek, kesin silinmesini garanti eder
          Cookies.remove("token", { path: "/" }); 
          set({ user: null, token: null, isAuthenticated: false });
        },

        // 3. PROFİL GÜNCELLEME
        updateUser: (updatedFields) =>
          set((state) => ({
            user: state.user ? { ...state.user, ...updatedFields } : null,
          })),
      }),
      {
        name: "bank-auth-storage",
      },
    ),
  ),
);