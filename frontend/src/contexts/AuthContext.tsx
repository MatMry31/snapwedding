import { createContext, ReactNode, useCallback, useContext, useEffect, useState } from "react";
import { LOGOUT_EVENT, TOKEN_KEY, USER_KEY } from "@/lib/auth-storage";

/**
 * Connexion de l'ORGANISATEUR avec Google.
 * Le "credential" renvoyé par Google est un ID token (JWT) : on le garde et on l'envoie
 * à l'API dans le header Authorization. C'est le backend qui vérifie sa signature.
 */

interface User {
  id: string;
  email: string;
  name: string;
  picture?: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  loginWithGoogle: (credential: string) => void;
  logout: () => void;
}

interface GoogleIdTokenPayload {
  sub: string;
  email: string;
  name?: string;
  picture?: string;
  /** Expiration, en secondes depuis 1970 */
  exp: number;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

/** Lit le contenu du JWT (sans vérifier la signature : ce n'est pas le rôle du front). */
function decodeGoogleCredential(credential: string): GoogleIdTokenPayload {
  const parts = credential.split(".");
  if (parts.length < 2) throw new Error("Invalid Google token");
  const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
  // decodeURIComponent/escape : gère les accents dans le nom
  return JSON.parse(decodeURIComponent(escape(atob(padded))));
}

function isExpired(credential: string) {
  try {
    return decodeGoogleCredential(credential).exp * 1000 < Date.now();
  } catch {
    return true;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);

  const logout = useCallback(() => {
    setUser(null);
    setToken(null);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(TOKEN_KEY);
  }, []);

  useEffect(() => {
    const storedUser = localStorage.getItem(USER_KEY);
    const storedToken = localStorage.getItem(TOKEN_KEY);
    // Le jeton Google expire au bout d'une heure : inutile de garder une session morte
    if (storedUser && storedToken && !isExpired(storedToken)) {
      setUser(JSON.parse(storedUser));
      setToken(storedToken);
    } else {
      logout();
    }

    window.addEventListener(LOGOUT_EVENT, logout);
    return () => window.removeEventListener(LOGOUT_EVENT, logout);
  }, [logout]);

  const loginWithGoogle = (credential: string) => {
    const payload = decodeGoogleCredential(credential);
    const googleUser: User = {
      id: payload.sub,
      email: payload.email,
      name: payload.name ?? payload.email,
      picture: payload.picture,
    };
    setUser(googleUser);
    setToken(credential);
    localStorage.setItem(USER_KEY, JSON.stringify(googleUser));
    localStorage.setItem(TOKEN_KEY, credential);
  };

  return (
    <AuthContext.Provider
      value={{ user, token, isAuthenticated: !!user && !!token, loginWithGoogle, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}
