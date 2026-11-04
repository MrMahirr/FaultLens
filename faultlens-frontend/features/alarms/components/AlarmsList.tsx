"use client";

import { useMemo, useState } from "react";
import { DataTable, type DataTableColumnDef } from "@/shared/components/ui/Table";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import { DropdownSelect } from "@/shared/components/ui/Dropdown";
import { Pagination } from "@/shared/components/ui/Pagination";
import { Alarm, AlarmFilters, AlarmStatus, ALARM_STATUS_LABELS } from "../types/alarm.types";
import { Severity } from "@/shared/types/common.types";
import { Check, Hand, Trash2 } from "lucide-react";
import { cn } from "@/shared/lib/utils";
import { AlarmDetailModal } from "./AlarmDetailModal";
import { useAlarmsQuery } from "../api/useAlarmsQuery";
import {
  useAcknowledgeAlarmMutation,
  useDeleteAlarmMutation,
  useResolveAlarmMutation,
} from "../api/useAlarmsMutation";

const PAGE_SIZE = 20;

const STATUS_OPTIONS = [
  { label: "Tüm durumlar", value: "" },
  ...Object.values(AlarmStatus).map((status) => ({
    label: ALARM_STATUS_LABELS[status],
    value: status,
  })),
];

const SEVERITY_OPTIONS = [
  { label: "Tüm seviyeler", value: "" },
  ...[Severity.CRITICAL, Severity.ERROR, Severity.WARN, Severity.INFO].map((severity) => ({
    label: severity,
    value: severity,
  })),
];

const STATUS_BADGE: Record<AlarmStatus, "error" | "warning" | "success"> = {
  [AlarmStatus.ACTIVE]: "error",
  [AlarmStatus.ACKNOWLEDGED]: "warning",
  [AlarmStatus.RESOLVED]: "success",
};

export function AlarmsList() {
  const [filters, setFilters] = useState<AlarmFilters>({ page: 0, size: PAGE_SIZE });
  const [selectedAlarm, setSelectedAlarm] = useState<Alarm | null>(null);

  const { data, isLoading } = useAlarmsQuery(filters);
  const acknowledgeMutation = useAcknowledgeAlarmMutation();
  const resolveMutation = useResolveAlarmMutation();
  const deleteMutation = useDeleteAlarmMutation();

  const alarms = data?.content ?? [];
  const isMutating =
    acknowledgeMutation.isPending || resolveMutation.isPending || deleteMutation.isPending;

  // Changing a filter always goes back to the first page.
  const updateFilter = (key: "status" | "severity", value: string) =>
    setFilters((current) => ({ ...current, [key]: value || undefined, page: 0 }));

  const columns = useMemo<DataTableColumnDef<Alarm>[]>(
    () => [
      {
        accessorKey: "ruleName",
        header: "Alarm Adı",
        cell: ({ row }) => (
          <div className="font-medium text-text-primary">{row.original.ruleName}</div>
        ),
      },
      {
        accessorKey: "severity",
        header: "Seviye",
        cell: ({ row }) => {
          const severity = row.original.severity;
          let variant: "error" | "warning" | "info" = "info";
          if (severity === Severity.CRITICAL || severity === Severity.ERROR) {
            variant = "error";
          } else if (severity === Severity.WARN) {
            variant = "warning";
          }
          return <Badge variant={variant}>{severity}</Badge>;
        },
      },
      {
        accessorKey: "message",
        header: "Mesaj",
        cell: ({ row }) => (
          <div className="text-text-muted truncate max-w-[300px]">{row.original.message}</div>
        ),
      },
      {
        accessorKey: "status",
        header: "Durum",
        cell: ({ row }) => {
          const status = row.original.status;
          return (
            <Badge
              variant={STATUS_BADGE[status]}
              className={cn(status === AlarmStatus.ACTIVE && "animate-pulse")}
            >
              {ALARM_STATUS_LABELS[status]}
            </Badge>
          );
        },
      },
      {
        accessorKey: "triggeredAt",
        header: "Tetiklenme Zamanı",
        cell: ({ row }) => new Date(row.original.triggeredAt).toLocaleString("tr-TR"),
      },
      {
        id: "actions",
        header: "Aksiyon",
        cell: ({ row }) => {
          const alarm = row.original;
          return (
            <div className="flex items-center gap-2">
              {alarm.status === AlarmStatus.ACTIVE && (
                <Button
                  variant="secondary"
                  size="sm"
                  icon={<Hand size={14} />}
                  onClick={(e) => {
                    e.stopPropagation();
                    acknowledgeMutation.mutate(alarm.id);
                  }}
                  disabled={isMutating}
                  title="Üstlen (ilgileniyorum)"
                >
                  Üstlen
                </Button>
              )}
              {alarm.status !== AlarmStatus.RESOLVED && (
                <Button
                  variant="secondary"
                  size="sm"
                  icon={<Check size={14} />}
                  onClick={(e) => {
                    e.stopPropagation();
                    resolveMutation.mutate(alarm.id);
                  }}
                  disabled={isMutating}
                  title="Çözüldü Olarak İşaretle"
                >
                  Çöz
                </Button>
              )}
              <Button
                variant="ghost"
                size="sm"
                className="text-error hover:bg-error/10 hover:text-error"
                icon={<Trash2 size={14} />}
                onClick={(e) => {
                  e.stopPropagation();
                  deleteMutation.mutate(alarm.id);
                }}
                disabled={isMutating}
                title="Sil"
              >
                Sil
              </Button>
            </div>
          );
        },
      },
    ],
    [acknowledgeMutation, resolveMutation, deleteMutation, isMutating],
  );

  return (
    <div className="space-y-4">
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <h2 className="text-xl font-semibold text-text-primary">Alarm Geçmişi</h2>
        <div className="flex gap-3">
          <DropdownSelect
            value={filters.status ?? ""}
            options={STATUS_OPTIONS}
            onChange={(value) => updateFilter("status", value)}
            className="min-w-[160px]"
          />
          <DropdownSelect
            value={filters.severity ?? ""}
            options={SEVERITY_OPTIONS}
            onChange={(value) => updateFilter("severity", value)}
            className="min-w-[160px]"
          />
        </div>
      </div>

      <DataTable
        data={alarms}
        columns={columns}
        emptyMessage="Herhangi bir alarm bulunmuyor."
        // Sorting is server-side (newest first); client sorting would only reorder one page.
        enableSorting={false}
        loading={isLoading}
        onRowClick={(row) => setSelectedAlarm(row)}
      />

      {data && data.totalPages > 1 && (
        <div className="flex justify-center">
          <Pagination
            currentPage={data.page + 1}
            totalPages={data.totalPages}
            onPageChange={(page) => setFilters((current) => ({ ...current, page: page - 1 }))}
          />
        </div>
      )}

      <AlarmDetailModal
        isOpen={!!selectedAlarm}
        onClose={() => setSelectedAlarm(null)}
        alarm={selectedAlarm}
      />
    </div>
  );
}
