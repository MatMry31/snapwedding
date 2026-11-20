import { getStoredToken, LOGOUT_EVENT } from "@/lib/auth-storage";

/* ---------- Types : le miroir des DTO du backend ---------- */

export type EventStatus = "OPEN" | "CLOSED" | "SUSPENDED";

/** EventResponse */
export interface SnapEvent {
  id: string;
  name: string;
  /** Date seule, ex. "2027-06-12" */
  eventDate: string;
  location: string | null;
  publicCode: string;
  status: EventStatus;
  /** ISO 8601 UTC */
  createdAt: string;
}

/** EventCreateRequest / EventUpdateRequest : uniquement les champs autorisés */
export interface EventInput {
  name: string;
  eventDate: string;
  location?: string;
}

/** AlbumResponse */
export interface Album {
  id: string;
  name: string;
  createdAt: string;
}

/** Format Spring Data "VIA_DTO" */
export interface PageResponse<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

/** Erreur renvoyée par l'ApiExceptionHandler : { "error": "...", "details"?: {...} } */
export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

/* ---------- Client HTTP ---------- */

const API_BASE_URL = ((import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "").replace(/\/$/, "");

async function request(path: string, options: RequestInit = {}, query?: Record<string, string | number>) {
  const token = getStoredToken();
  const headers: Record<string, string> = {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.body && !(options.body instanceof FormData) ? { "Content-Type": "application/json" } : {}),
  };
  const search = query ? `?${new URLSearchParams(Object.entries(query).map(([k, v]) => [k, String(v)]))}` : "";

  const response = await fetch(`${API_BASE_URL}${path}${search}`, { ...options, headers });

  if (response.status === 401) {
    window.dispatchEvent(new Event(LOGOUT_EVENT)); // jeton expiré : retour à la connexion
  }
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const details = body?.details ? ` (${Object.entries(body.details).map(([k, v]) => `${k} : ${v}`).join(", ")})` : "";
    throw new ApiError(response.status, (body?.error ?? `Erreur ${response.status}`) + details);
  }
  return response;
}

async function json<T>(path: string, options: RequestInit = {}, query?: Record<string, string | number>): Promise<T> {
  const response = await request(path, options, query);
  return (response.status === 204 ? undefined : await response.json()) as T;
}

/* ---------- Événements ---------- */

export const api = {
  getEvents: (page = 0, size = 20) =>
    json<PageResponse<SnapEvent>>("/api/events", {}, { page, size, sort: "eventDate,asc" }),

  getEvent: (id: string) => json<SnapEvent>(`/api/events/${id}`),

  createEvent: (input: EventInput) =>
    json<SnapEvent>("/api/events", { method: "POST", body: JSON.stringify(input) }),

  updateEvent: (id: string, input: EventInput) =>
    json<SnapEvent>(`/api/events/${id}`, { method: "PUT", body: JSON.stringify(input) }),

  closeEvent: (id: string) => json<SnapEvent>(`/api/events/${id}/close`, { method: "POST" }),

  deleteEvent: (id: string) => json<void>(`/api/events/${id}`, { method: "DELETE" }),

  /** Le QR code est protégé par le jeton : on le télécharge puis on en fait une URL locale (blob:). */
  getQrCodeUrl: async (id: string) => URL.createObjectURL(await (await request(`/api/events/${id}/qrcode`)).blob()),

  /* ---------- Albums ---------- */

  getAlbums: (eventId: string) => json<Album[]>(`/api/events/${eventId}/albums`),

  createAlbum: (eventId: string, name: string) =>
    json<Album>(`/api/events/${eventId}/albums`, { method: "POST", body: JSON.stringify({ name }) }),

  renameAlbum: (eventId: string, albumId: string, name: string) =>
    json<Album>(`/api/events/${eventId}/albums/${albumId}`, { method: "PUT", body: JSON.stringify({ name }) }),

  deleteAlbum: (eventId: string, albumId: string) =>
    json<void>(`/api/events/${eventId}/albums/${albumId}`, { method: "DELETE" }),
};
