import { Link, Outlet } from "react-router-dom";
import { Camera, KeyRound, LogOut } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";

export function AppLayout() {
  const { user, token, logout } = useAuth();

  /** Outil de développement : copie le jeton pour le coller dans Swagger (bouton Authorize). */
  const copyToken = async () => {
    if (!token) return;
    await navigator.clipboard.writeText(token);
    toast.success("Jeton copié : collez-le dans Swagger > Authorize");
  };

  return (
    <div className="min-h-screen flex flex-col bg-muted/30">
      <header className="border-b bg-background sticky top-0 z-10">
        <div className="container mx-auto px-4 py-3 flex items-center justify-between gap-4">
          <Link to="/events" className="flex items-center gap-2 text-lg font-bold">
            <Camera className="h-5 w-5" />
            SnapWedding
          </Link>
          <div className="flex items-center gap-2 sm:gap-4">
            {import.meta.env.DEV && (
              <Button variant="ghost" size="sm" onClick={copyToken} title="Copier le jeton pour Swagger">
                <KeyRound className="h-4 w-4" />
                <span className="hidden md:inline">Jeton Swagger</span>
              </Button>
            )}
            <div className="hidden sm:flex items-center gap-2">
              <Avatar className="h-8 w-8">
                <AvatarImage src={user?.picture} alt={user?.name} />
                <AvatarFallback>{user?.name.substring(0, 2).toUpperCase()}</AvatarFallback>
              </Avatar>
              <span className="text-sm font-medium">{user?.name}</span>
            </div>
            <Button variant="outline" size="sm" onClick={logout}>
              <LogOut className="h-4 w-4" />
              <span className="hidden sm:inline">Déconnexion</span>
            </Button>
          </div>
        </div>
      </header>
      <main className="flex-1 container mx-auto px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
