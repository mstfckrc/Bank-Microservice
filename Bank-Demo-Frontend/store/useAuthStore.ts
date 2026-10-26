import { create } from "zustand";
import { devtools } from "zustand/middleware";
import type { AccessRestrictionStatus, ApprovalStatus, Role, UserProfileResponse } from "@/types";

export interface AuthUser {
  identityNumber: string;
  profileName: string;
  email: string;
  role: Role;
  status: ApprovalStatus;
  accessRestrictionStatus?: AccessRestrictionStatus;
}

interface AuthState {
  user: AuthUser | null;
  csrfToken: string | null;
  idleExpiresAt: number | null;
  absoluteExpiresAt: number | null;
  isAuthenticated: boolean;
  login: (user: UserProfileResponse, csrfToken: string, idleExpiresAt: number, absoluteExpiresAt: number) => void;
  updateSession: (idleExpiresAt: number, absoluteExpiresAt: number) => void;
  logout: () => void;
  updateUser: (updatedFields: Partial<AuthUser>) => void;
}

function clearLegacyBrowserCredentials(): void {
  if (typeof document === "undefined") return;
  try {
    window.localStorage.removeItem("bank-auth-storage");
  } catch {
    // Storage may be unavailable; authentication never depends on it.
  }
  for (const name of ["token", "id_token"]) {
    document.cookie = `${name}=; Max-Age=0; Path=/; SameSite=Lax`;
  }
}

function asAuthUser(user: UserProfileResponse): AuthUser {
  return {
    identityNumber: user.identityNumber,
    profileName: user.profileName,
    email: user.email,
    role: user.role,
    status: user.status,
    accessRestrictionStatus: user.accessRestrictionStatus,
  };
}

if (typeof window !== "undefined") clearLegacyBrowserCredentials();

export const useAuthStore = create<AuthState>()(
  devtools(
    (set) => ({
      user: null,
      csrfToken: null,
      idleExpiresAt: null,
      absoluteExpiresAt: null,
      isAuthenticated: false,

      login: (user, csrfToken, idleExpiresAt, absoluteExpiresAt) => {
        clearLegacyBrowserCredentials();
        set({
          user: asAuthUser(user),
          csrfToken,
          idleExpiresAt,
          absoluteExpiresAt,
          isAuthenticated: true,
        });
      },

      updateSession: (idleExpiresAt, absoluteExpiresAt) => set({ idleExpiresAt, absoluteExpiresAt }),

      logout: () => {
        clearLegacyBrowserCredentials();
        set({
          user: null,
          csrfToken: null,
          idleExpiresAt: null,
          absoluteExpiresAt: null,
          isAuthenticated: false,
        });
      },

      updateUser: (updatedFields) => set((state) => ({
        user: state.user ? { ...state.user, ...updatedFields } : null,
      })),
    }),
  ),
);
