"use client";

import { motion } from "framer-motion";
import { AlertTriangle, Server, Layers, Brain } from "lucide-react";
import { Card } from "@/shared/components/ui/Card";
import { SkeletonCard } from "@/shared/components/ui/Skeleton";
import { useDashboardStatsQuery } from "@/features/dashboard/api/useDashboardQuery";
import { formatNumber } from "@/shared/lib/utils";

/* ── Types ─────────────────────────────────────────────────── */

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle: string;
  icon: React.ReactNode;
  iconBg: string;
  index: number;
  change?: number;
}

/* ── StatCard ──────────────────────────────────────────────── */

function StatCard({
  title,
  value,
  subtitle,
  icon,
  iconBg,
  index,
  change,
}: StatCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.1, duration: 0.4 }}
    >
      <Card variant="default" hover>
        <div className="flex items-start justify-between">
          <div className="space-y-2">
            <p className="text-xs font-medium text-text-muted uppercase tracking-wider">
              {title}
            </p>
            <p className="text-3xl font-bold font-display text-text-primary">
              {typeof value === "number" ? formatNumber(value) : value}
            </p>
            <div className="flex items-center gap-2">
              <p className="text-xs text-text-secondary">{subtitle}</p>
              {change !== undefined && (
                <span
                  className={`text-xs font-mono ${
                    change > 0 ? "text-error" : "text-success"
                  }`}
                >
                  {change > 0 ? "↑" : "↓"} {Math.abs(change)}%
                </span>
              )}
            </div>
          </div>
          <div
            className={`shrink-0 w-11 h-11 rounded-xl flex items-center justify-center ${iconBg}`}
          >
            {icon}
          </div>
        </div>
      </Card>
    </motion.div>
  );
}

/* ── StatsGrid ─────────────────────────────────────────────── */

function StatsGrid() {
  const { data: stats, isLoading } = useDashboardStatsQuery();

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {Array.from({ length: 4 }, (_, i) => (
          <SkeletonCard key={i} />
        ))}
      </div>
    );
  }

  if (!stats) return null;

  const cards: Omit<StatCardProps, "index">[] = [
    {
      title: "Toplam Hata",
      value: stats.totalErrors,
      subtitle: "Son 1 saat",
      icon: <AlertTriangle size={20} className="text-error" />,
      iconBg: "bg-error/10",
      change: stats.totalErrorsChange,
    },
    {
      title: "Aktif Kaynaklar",
      value: `${stats.activeSources}/${stats.totalSources}`,
      subtitle: "Bağlı / Toplam",
      icon: <Server size={20} className="text-success" />,
      iconBg: "bg-success/10",
    },
    {
      title: "Log Grupları",
      value: stats.openGroups,
      subtitle: "Açık gruplar",
      icon: <Layers size={20} className="text-warning" />,
      iconBg: "bg-warning/10",
    },
    {
      title: "Analizler",
      value: stats.analysesToday,
      subtitle: "Bugün",
      icon: <Brain size={20} className="text-accent" />,
      iconBg: "bg-accent/10",
    },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card, index) => (
        <StatCard key={card.title} {...card} index={index} />
      ))}
    </div>
  );
}

export { StatsGrid };
