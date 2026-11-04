"use client";

import { motion } from "framer-motion";
import { Sparkles, ArrowRight } from "lucide-react";
import Link from "next/link";
import { Button } from "@/shared/components/ui/Button";
import { useAnalysesQuery } from "@/features/analyses/api/useAnalysisQuery";

function AiSummaryWidget() {
  const { data: analysisData, isLoading } = useAnalysesQuery({ page: 0, size: 1 });

  const totalElements = analysisData?.totalElements || 0;
  const latestAnalysis = analysisData?.content?.[0];
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
              FaultLens AI Analizi
            </h2>
            {isLoading ? (
              <div className="space-y-2 mt-2">
                <div className="h-4 w-64 bg-bg-tertiary rounded animate-pulse" />
                <div className="h-4 w-48 bg-bg-tertiary rounded animate-pulse" />
              </div>
            ) : totalElements > 0 ? (
              <p className="text-sm text-text-secondary leading-relaxed max-w-2xl">
                Sistemde toplam{" "}
                <strong className="text-text-primary">{totalElements} hata analizi</strong>{" "}
                gerçekleştirildi.
                {latestAnalysis && (
                  <>
                    {" "}
                    En son yapılan analiz
                    {latestAnalysis.affectedDeployment ? (
                      <>
                        {" "}
                        <strong className="text-text-primary">
                          {latestAnalysis.affectedDeployment}
                        </strong>{" "}
                        deployment&apos;ı ile
                      </>
                    ) : (
                      " genel sistem hataları ile"
                    )}{" "}
                    ilişkili. Yapay zeka kök neden analizini tamamladı.
                  </>
                )}
              </p>
            ) : (
              <p className="text-sm text-text-secondary leading-relaxed max-w-2xl">
                Şu an için sistemde yapay zeka tarafından incelenmiş bir hata veya anomali
                bulunmuyor. Sistem durumu stabil.
              </p>
            )}
          </div>
        </div>

        <div className="shrink-0 flex items-center gap-3">
          <Link href="/analyses">
            <Button
              variant="primary"
              size="md"
              icon={<ArrowRight size={16} />}
              iconPosition="right"
            >
              {totalElements > 0 ? "Analizi İncele" : "Analiz Sayfasına Git"}
            </Button>
          </Link>
        </div>
      </div>
    </motion.div>
  );
}

export { AiSummaryWidget };
