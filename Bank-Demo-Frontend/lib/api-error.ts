import { isAxiosError } from "axios";

interface ApiErrorPayload {
  message?: string;
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (!isAxiosError<ApiErrorPayload>(error)) {
    return fallback;
  }

  return error.response?.data?.message ?? fallback;
}
