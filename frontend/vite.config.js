import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

// "@" maps to /src so imports look like "@/components/ui/button" (shadcn convention)
export default defineConfig({
  plugins: [react()],
  resolve: { alias: { "@": path.resolve(__dirname, "./src") } },
});
