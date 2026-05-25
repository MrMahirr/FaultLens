"use client";

import { Hexagon } from "lucide-react";
import { LoginForm } from "@/features/auth/components/LoginForm";

export default function LoginPage() {
  return (
    <div className="glass rounded-2xl p-8 space-y-6">
      {/* Logo + Title */}
      <div className="text-center space-y-3">
        <div className="flex items-center justify-center gap-2">
          <div className="w-10 h-10 rounded-xl bg-accent/10 flex items-center justify-center">
            <Hexagon size={22} className="text-accent" />
          </div>
        </div>
        <div>
          <h1 className="text-2xl font-bold font-display text-text-primary">
            Log<span className="text-accent">Lens</span>
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Log analiz platformuna giriş yapın
          </p>
        </div>
      </div>

      {/* Login Form */}
      <LoginForm />
    </div>
  );
}
