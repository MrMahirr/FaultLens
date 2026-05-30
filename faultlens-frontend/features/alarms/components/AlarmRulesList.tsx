"use client";

import { useMemo } from "react";
import { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/shared/components/ui/Table";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import { AlarmRule } from "../types/alarm.types";
import { Severity } from "@/shared/types/common.types";
import { Power, PowerOff, Trash2 } from "lucide-react";
import { useAlarmStore } from "../store/alarm.store";

export function AlarmRulesList() {
  const { rules, toggleRule, deleteRule } = useAlarmStore();

  const columns = useMemo<ColumnDef<AlarmRule>[]>(
    () => [
      {
        accessorKey: "name",
        header: "Kural Adı",
        cell: ({ row }) => (
          <div className="font-medium text-text-primary">
            {row.original.name}
          </div>
        ),
      },
      {
        accessorKey: "condition",
        header: "Koşul",
        cell: ({ row }) => (
          <code className="px-2 py-1 bg-bg-tertiary rounded text-xs text-text-secondary font-mono">
            {row.original.condition}
          </code>
        ),
      },
      {
        accessorKey: "severity",
        header: "Tetiklenecek Seviye",
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
        accessorKey: "isActive",
        header: "Durum",
        cell: ({ row }) => {
          const isActive = row.original.isActive;
          return (
            <Badge variant={isActive ? "success" : "secondary"}>
              {isActive ? "Aktif" : "Pasif"}
            </Badge>
          );
        },
      },
      {
        id: "actions",
        header: "Aksiyon",
        cell: ({ row }) => {
          const rule = row.original;
          return (
            <div className="flex items-center gap-2">
              <Button
                variant="ghost"
                size="sm"
                icon={rule.isActive ? <PowerOff size={14} /> : <Power size={14} />}
                onClick={(e) => {
                  e.stopPropagation();
                  toggleRule(rule.id);
                }}
                title={rule.isActive ? "Pasife Al" : "Aktifleştir"}
                className={rule.isActive ? "text-warning" : "text-success"}
              >
                {rule.isActive ? "Pasife Al" : "Aktifleştir"}
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="text-error hover:bg-error/10 hover:text-error"
                icon={<Trash2 size={14} />}
                onClick={(e) => {
                  e.stopPropagation();
                  deleteRule(rule.id);
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
    [toggleRule, deleteRule]
  );

  return (
    <div className="space-y-4">
      <DataTable
        data={rules}
        columns={columns}
        emptyMessage="Herhangi bir kural bulunmuyor."
        enableSorting={true}
      />
    </div>
  );
}
