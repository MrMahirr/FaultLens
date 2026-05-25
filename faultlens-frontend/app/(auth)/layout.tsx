"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/features/auth/store/auth.store";

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const token = useAuthStore((state) => state.token);
  const router = useRouter();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    if (token) {
      router.replace("/dashboard");
    }
  }, [token, router]);

  if (!mounted) return null;

  // If already authenticated, show empty loader during transition
  if (token) {
    return (
      <div className="min-h-screen bg-bg-primary flex items-center justify-center">
        <div className="w-10 h-10 border-2 border-accent border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-bg-primary bg-grid-pattern bg-accent-glow flex items-center justify-center p-4">
      <div className="relative z-10 w-full max-w-md">{children}</div>
    </div>
  );
}
