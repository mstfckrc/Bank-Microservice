import axios from "axios";
import { toast } from "sonner";
import { useAuthStore } from "@/store/useAuthStore";

const api = axios.create({
  baseURL: "/api/bank",
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  const method = config.method?.toLowerCase();
  const writes = method === "post" || method === "put" || method === "patch" || method === "delete";
  const csrfToken = useAuthStore.getState().csrfToken;
  if (writes && csrfToken) config.headers.set("X-CSRF-Token", csrfToken);
  return config;
});

let sessionRedirectInProgress = false;

api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (!axios.isAxiosError(error)) return Promise.reject(error);

    const status = error.response?.status;
    const code = error.response?.data?.code;
    const message = error.response?.data?.message || "Beklenmedik bir hata oluştu.";

    if (status === 403 && code === "ACCOUNT_ACCESS_RESTRICTED") {
      useAuthStore.getState().updateUser({ accessRestrictionStatus: "RESTRICTED" });
      if (typeof window !== "undefined" && window.location.pathname !== "/account-restricted") {
        window.location.assign("/account-restricted");
      }
    } else if (status === 403) {
      toast.error("İşlem için yetkiniz yok", { description: message });
    } else if (status === 401) {
      const auth = useAuthStore.getState();
      if (auth.isAuthenticated && typeof window !== "undefined" && !sessionRedirectInProgress) {
        sessionRedirectInProgress = true;
        auth.logout();
        window.location.assign("/login?session=expired");
      }
    } else {
      toast.error("İşlem Başarısız", { description: message });
    }

    return Promise.reject(error);
  },
);

export default api;
