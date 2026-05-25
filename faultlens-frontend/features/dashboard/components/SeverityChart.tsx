"use client";

import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { Card } from "@/shared/components/ui/Card";
import { Skeleton } from "@/shared/components/ui/Skeleton";
import { useSeverityChart } from "@/features/dashboard/hooks/useDashboardStats";

/* ── Custom Tooltip ────────────────────────────────────────── */

interface ChartTooltipProps {
  active?: boolean;
  payload?: Array<{
    name: string;
    value: number;
    color: string;
  }>;
  label?: string;
}

function ChartTooltip({ active, payload, label }: ChartTooltipProps) {
  if (!active || !payload) return null;

  return (
    <div className="bg-bg-secondary border border-border-default rounded-xl px-4 py-3 shadow-lg">
      <p className="text-xs text-text-muted mb-2 font-mono">{label} ago</p>
      {payload.map((entry) => (
        <div key={entry.name} className="flex items-center justify-between gap-4 text-xs">
          <div className="flex items-center gap-1.5">
            <span
              className="w-2 h-2 rounded-full"
              style={{ backgroundColor: entry.color }}
            />
            <span className="text-text-secondary capitalize">{entry.name}</span>
          </div>
          <span className="font-mono font-medium text-text-primary">
            {entry.value}
          </span>
        </div>
      ))}
    </div>
  );
}

/* ── Component ─────────────────────────────────────────────── */

function SeverityChart() {
  const { data: chartData, isLoading } = useSeverityChart();

  return (
    <Card variant="default">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="text-sm font-semibold font-display text-text-primary">
            Severity Dağılımı
          </h3>
          <p className="text-xs text-text-muted mt-0.5">Son 1 saat</p>
        </div>
      </div>

      {isLoading ? (
        <Skeleton variant="rectangular" className="w-full h-[280px]" />
      ) : (
        <ResponsiveContainer width="100%" height={280}>
          <AreaChart data={chartData}>
            <defs>
              <linearGradient id="gradCritical" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#DC2626" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#DC2626" stopOpacity={0} />
              </linearGradient>
              <linearGradient id="gradError" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#EF4444" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#EF4444" stopOpacity={0} />
              </linearGradient>
              <linearGradient id="gradWarn" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#F59E0B" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#F59E0B" stopOpacity={0} />
              </linearGradient>
              <linearGradient id="gradInfo" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#3B82F6" stopOpacity={0} />
              </linearGradient>
            </defs>

            <CartesianGrid
              strokeDasharray="3 3"
              stroke="var(--border)"
              vertical={false}
            />
            <XAxis
              dataKey="time"
              tick={{ fill: "var(--text-muted)", fontSize: 11 }}
              axisLine={false}
              tickLine={false}
            />
            <YAxis
              tick={{ fill: "var(--text-muted)", fontSize: 11 }}
              axisLine={false}
              tickLine={false}
              width={30}
            />
            <Tooltip content={<ChartTooltip />} />
            <Legend
              iconType="circle"
              iconSize={8}
              wrapperStyle={{ fontSize: 11, color: "var(--text-secondary)" }}
            />

            <Area
              type="monotone"
              dataKey="critical"
              stroke="#DC2626"
              fill="url(#gradCritical)"
              strokeWidth={2}
              dot={false}
              name="Critical"
            />
            <Area
              type="monotone"
              dataKey="error"
              stroke="#EF4444"
              fill="url(#gradError)"
              strokeWidth={2}
              dot={false}
              name="Error"
            />
            <Area
              type="monotone"
              dataKey="warn"
              stroke="#F59E0B"
              fill="url(#gradWarn)"
              strokeWidth={2}
              dot={false}
              name="Warning"
            />
            <Area
              type="monotone"
              dataKey="info"
              stroke="#3B82F6"
              fill="url(#gradInfo)"
              strokeWidth={2}
              dot={false}
              name="Info"
            />
          </AreaChart>
        </ResponsiveContainer>
      )}
    </Card>
  );
}

export { SeverityChart };
