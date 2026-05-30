"use client";

import { useMemo } from "react";
import { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/shared/components/ui/Table";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import { Alarm, AlarmStatus } from "../types/alarm.types";
import { Severity } from "@/shared/types/common.types";
import { Check, Trash2 } from "lucide-react";
import { useAlarmStore } from "../store/alarm.store";
import { cn } from "@/shared/lib/utils";

export function AlarmsList() {
  const { alarms, resolveAlarm, deleteAlarm } = useAlarmStore();

  const columns = useMemo<ColumnDef<Alarm>[]>(
    () => [
      {
        accessorKey: "ruleName",
        header: "Alarm Adı",
        cell: ({ row }) => (
          <div className="font-medium text-text-primary">
            {row.original.ruleName}
          </div>
        ),
      },
      {
        accessorKey: "severity",
        header: "Seviye",
        cell: ({ row }) => {
          const severity = row.original.severity;
          let variant: "error" | "warning" | "info" | "success" = "info";
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
          <div className="text-text-muted truncate max-w-[300px]">
            {row.original.message}
          </div>
        ),
      },
      {
        accessorKey: "status",
        header: "Durum",
        cell: ({ row }) => {
          const status = row.original.status;
          return (
            <Badge
              variant={status === AlarmStatus.ACTIVE ? "error" : "success"}
              className={cn(
                status === AlarmStatus.ACTIVE && "animate-pulse"
              )}
            >
              {status}
            </Badge>
          );
        },
      },
      {
        accessorKey: "triggeredAt",
        header: "Tetiklenme Zamanı",
        cell: ({ row }) => {
          return new Date(row.original.triggeredAt).toLocaleString("tr-TR");
        },
      },
      {
        id: "actions",
        header: "Aksiyon",
        cell: ({ row }) => {
          const alarm = row.original;
          const isActive = alarm.status === AlarmStatus.ACTIVE;

          return (
            <div className="flex items-center gap-2">
              {isActive && (
                <Button
                  variant="secondary"
                  size="sm"
                  icon={<Check size={14} />}
                  onClick={(e) => {
                    e.stopPropagation();
                    resolveAlarm(alarm.id);
                  }}
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
                  deleteAlarm(alarm.id);
                }}
                title="Sil"
              >
                Sil
              </Button>
            </div>
          );
        },
      },
    ],
    [resolveAlarm, deleteAlarm]
  );

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-text-primary">
          Alarm Geçmişi
        </h2>
      </div>
      <DataTable
        data={alarms}
        columns={columns}
        emptyMessage="Herhangi bir alarm bulunmuyor."
        enableSorting={true}
      />
    </div>
  );
}
