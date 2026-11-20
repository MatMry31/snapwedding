/**
 * Stockage de la session de l'organisateur dans le navigateur.
 * Isolé ici pour être partagé entre AuthContext (React) et le client API (hors React).
 */

export const USER_KEY = "snapwedding_user";
export const TOKEN_KEY = "snapwedding_token";

/** Émis par le client API sur une réponse 401 : l'AuthContext déconnecte l'organisateur. */
export const LOGOUT_EVENT = "snapwedding:logout";

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY);
}
