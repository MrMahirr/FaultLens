"use client";

import { motion } from "framer-motion";
import { Sparkles, ArrowRight } from "lucide-react";
import Link from "next/link";
import { Button } from "@/shared/components/ui/Button";

function AiSummaryWidget() {
  return (
    <motion.div
      initial={{ opacity: 0, y: -20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-accent/20 via-bg-secondary to-bg-secondary border border-accent/20 p-6"
    >
      {/* Background Glow */}
      <div className="absolute top-0 left-0 w-64 h-64 bg-accent/20 rounded-full blur-[80px] -translate-x-1/2 -translate-y-1/2 pointer-events-none" />

      <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-xl bg-accent/20 flex items-center justify-center shrink-0 border border-accent/30 shadow-[0_0_15px_rgba(var(--accent-rgb),0.3)]">
            <Sparkles size={24} className="text-accent animate-pulse" />
          </div>
          <div>
            <h2 className="text-lg font-bold font-display text-text-primary mb-1">
              LogLens AI Analizi
            </h2>
            <p className="text-sm text-text-secondary leading-relaxed max-w-2xl">
              Son 24 saat içinde <strong className="text-text-primary">3 yeni hata grubu</strong> tespit edildi.
              Bunlardan biri son yapılan <strong className="text-text-primary">payment-service v2.3.1</strong> deployment'ı ile doğrudan ilişkili. Yapay zeka kök neden analizini tamamladı.
            </p>
          </div>
        </div>

        <div className="shrink-0 flex items-center gap-3">
          <Link href="/analyses">
            <Button variant="primary" size="md" icon={<ArrowRight size={16} />} iconPosition="right">
              Analizi İncele
            </Button>
          </Link>
        </div>
      </div>
    </motion.div>
  );
}

export { AiSummaryWidget };
