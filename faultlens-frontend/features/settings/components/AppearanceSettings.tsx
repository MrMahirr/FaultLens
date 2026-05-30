"use client";

import { useUIStore } from "@/shared/store/ui.store";
import { Button } from "@/shared/components/ui/Button";
import { Moon, Sun, MonitorSmartphone } from "lucide-react";

export function AppearanceSettings() {
  const { theme, setTheme, compactMode, setCompactMode } = useUIStore();

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h2 className="text-lg font-semibold text-text-primary">Görünüm</h2>
        <p className="text-sm text-text-muted mt-1">
          Arayüz temasını ve görüntü tercihlerini ayarlayın.
        </p>
      </div>

      <div className="bg-bg-tertiary/30 p-6 rounded-2xl border border-border-default space-y-6">
        {/* Theme Selection */}
        <div>
          <h3 className="text-sm font-medium text-text-secondary mb-4">
            Tema Seçimi
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            {/* Light Mode */}
            <button
              onClick={() => setTheme("light")}
              className={`flex flex-col items-center justify-center p-6 rounded-xl border-2 transition-all duration-200 cursor-pointer ${
                theme === "light"
                  ? "border-accent bg-accent/5"
                  : "border-border-default hover:border-accent/40 bg-bg-primary"
              }`}
            >
              <Sun
                size={28}
                className={theme === "light" ? "text-accent" : "text-text-muted"}
              />
              <span
                className={`mt-3 font-medium ${
                  theme === "light" ? "text-accent" : "text-text-secondary"
                }`}
              >
                Açık (Light)
              </span>
            </button>

            {/* Dark Mode */}
            <button
              onClick={() => setTheme("dark")}
              className={`flex flex-col items-center justify-center p-6 rounded-xl border-2 transition-all duration-200 cursor-pointer ${
                theme === "dark"
                  ? "border-accent bg-accent/5"
                  : "border-border-default hover:border-accent/40 bg-bg-primary"
              }`}
            >
              <Moon
                size={28}
                className={theme === "dark" ? "text-accent" : "text-text-muted"}
              />
              <span
                className={`mt-3 font-medium ${
                  theme === "dark" ? "text-accent" : "text-text-secondary"
                }`}
              >
                Koyu (Dark)
              </span>
            </button>

            {/* System Mode */}
            <button
              onClick={() => setTheme("system")}
              className={`flex flex-col items-center justify-center p-6 rounded-xl border-2 transition-all duration-200 cursor-pointer ${
                theme === "system"
                  ? "border-accent bg-accent/5"
                  : "border-border-default hover:border-accent/40 bg-bg-primary"
              }`}
            >
              <MonitorSmartphone
                size={28}
                className={theme === "system" ? "text-accent" : "text-text-muted"}
              />
              <span
                className={`mt-3 font-medium ${
                  theme === "system" ? "text-accent" : "text-text-secondary"
                }`}
              >
                Sistem (Otomatik)
              </span>
            </button>
          </div>
        </div>

        <hr className="border-border-default" />

        {/* Compact View */}
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-sm font-medium text-text-primary">
              Kompakt Görünüm
            </h3>
            <p className="text-xs text-text-muted mt-1">
              Tablolarda daha dar satır aralıkları kullanarak daha fazla veri görün.
            </p>
          </div>
          <Button 
            variant={compactMode ? "primary" : "secondary"} 
            size="sm" 
            onClick={() => setCompactMode(!compactMode)}
          >
            {compactMode ? "Kapat" : "Etkinleştir"}
          </Button>
        </div>
      </div>
    </div>
  );
}
