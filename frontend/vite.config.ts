import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig({
  server: {
    host: "::",
    port: 5173,
    // Le port 5173 est le seul autorisé par Google (origine OAuth) et par le CORS du backend :
    // s'il est occupé, on préfère une erreur claire à un démarrage silencieux sur 5174.
    strictPort: true,
  },
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
});
