"use client";

import { useQuery } from "@tanstack/react-query";
import {
  mockDashboardStats,
  type MockDashboardStats,
  mockSeverityChartData,
  type MockChartPoint,
} from "@/shared/mocks/data";

/* ── Query Keys ────────────────────────────────────────────── */

export const dashboardKeys = {
  all: ["dashboard"] as const,
  stats: () => [...dashboardKeys.all, "stats"] as const,
  chart: () => [...dashboardKeys.all, "chart"] as const,
};

/* ── Mock Fetchers ─────────────────────────────────────────── */

const fetchDashboardStats = async (): Promise<MockDashboardStats> => {
  await new Promise((resolve) => setTimeout(resolve, 500));
  return mockDashboardStats;
};

const fetchSeverityChart = async (): Promise<MockChartPoint[]> => {
  await new Promise((resolve) => setTimeout(resolve, 600));
  return mockSeverityChartData;
};

/* ── Hooks ─────────────────────────────────────────────────── */

export const useDashboardStats = () =>
  useQuery({
    queryKey: dashboardKeys.stats(),
    queryFn: fetchDashboardStats,
  });

export const useSeverityChart = () =>
  useQuery({
    queryKey: dashboardKeys.chart(),
    queryFn: fetchSeverityChart,
  });
