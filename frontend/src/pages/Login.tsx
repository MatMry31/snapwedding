import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { CredentialResponse, GoogleLogin } from "@react-oauth/google";
import { Camera } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

/**
 * Après la connexion, on renvoie l'organisateur sur la page qu'il demandait.
 * Anti open redirect (OWASP) : uniquement un chemin interne ("/events/..."), jamais
 * "//site-pirate.com" ni "/\site-pirate.com", que le navigateur traiterait comme un autre site.
 */
function safeInternalPath(path: string | undefined) {
  return path && /^\/(?![/\\])/.test(path) ? path : "/events";
}

export default function Login() {
  const { loginWithGoogle, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState<string | null>(null);
  const targetPath = safeInternalPath((location.state as { from?: string } | null)?.from);

  useEffect(() => {
    if (isAuthenticated) navigate(targetPath, { replace: true });
  }, [isAuthenticated, navigate, targetPath]);

  const handleSuccess = (response: CredentialResponse) => {
    if (!response.credential) {
      setError("Réponse de Google invalide.");
      return;
    }
    loginWithGoogle(response.credential);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center space-y-2">
          <Camera className="h-10 w-10 mx-auto" />
          <CardTitle className="text-3xl font-bold">SnapWedding</CardTitle>
          <CardDescription>
            Espace organisateur : créez votre événement, imprimez le QR code, et récupérez toutes les photos de vos invités.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {error && (
            <p className="rounded-md border border-destructive/50 bg-destructive/10 px-3 py-2 text-sm text-destructive">
              {error}
            </p>
          )}
          {import.meta.env.VITE_GOOGLE_CLIENT_ID ? (
            <div className="flex justify-center">
              <GoogleLogin onSuccess={handleSuccess} onError={() => setError("La connexion Google a échoué.")} />
            </div>
          ) : (
            <p className="rounded-md border px-3 py-2 text-sm">
              Renseignez VITE_GOOGLE_CLIENT_ID dans frontend/.env pour activer la connexion.
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
