"use client";

import {
  createSortedRowModel,
  flexRender,
  rowSelectionFeature,
  rowSortingFeature,
  sortFns,
  tableFeatures,
  useTable,
  type ColumnDef,
  type RowSelectionState,
  type SortingState,
} from "@tanstack/react-table";
import { useState, useCallback } from "react";
import { ArrowUp, ArrowDown, ArrowUpDown } from "lucide-react";
import { useTranslations } from "next-intl";
import { cn } from "@/shared/lib/utils";
import { useUIStore } from "@/shared/store/ui.store";

/* ── Features ──────────────────────────────────────────────── */

/**
 * Table v9 registers features explicitly. DataTable uses sorting and row selection; the
 * built-in sort functions keep v8's automatic per-column sort choice. Module-level so the
 * object is stable across renders.
 */
const dataTableFeatures = tableFeatures({
  rowSortingFeature,
  rowSelectionFeature,
  sortedRowModel: createSortedRowModel(),
  sortFns,
});

/** Column definition for {@link DataTable}; use this instead of react-table's `ColumnDef`. */
type DataTableColumnDef<TData extends object> = ColumnDef<typeof dataTableFeatures, TData, unknown>;

/* ── Types ─────────────────────────────────────────────────── */

interface DataTableProps<TData extends object> {
  data: TData[];
  columns: DataTableColumnDef<TData>[];
  onRowClick?: (row: TData) => void;
  enableSorting?: boolean;
  enableSelection?: boolean;
  loading?: boolean;
  emptyMessage?: string;
  className?: string;
  stickyHeader?: boolean;
  /** Accessible name of the table. */
  label?: string;
}

/* ── Component (Instrument card + .th / .tr look) ──────────── */

function DataTable<TData extends object>({
  data,
  columns,
  onRowClick,
  enableSorting = true,
  enableSelection = false,
  loading = false,
  emptyMessage,
  className,
  stickyHeader = false,
  label,
}: DataTableProps<TData>) {
  const t = useTranslations("common");
  const [sorting, setSorting] = useState<SortingState>([]);
  const [rowSelection, setRowSelection] = useState<RowSelectionState>({});
  const compactMode = useUIStore((state) => state.compactMode);

  const table = useTable({
    features: dataTableFeatures,
    data,
    columns,
    state: { sorting, rowSelection },
    onSortingChange: setSorting,
    onRowSelectionChange: setRowSelection,
    enableSorting,
    enableRowSelection: enableSelection,
  });

  const handleRowClick = useCallback((row: TData) => onRowClick?.(row), [onRowClick]);

  return (
    <div className={cn("card w-full overflow-x-auto", className)}>
      <table className="w-full text-[13px]" aria-label={label} aria-busy={loading || undefined}>
        <thead className={cn("bg-bg-tertiary", stickyHeader && "sticky top-0 z-10")}>
          {table.getHeaderGroups().map((headerGroup) => (
            <tr key={headerGroup.id}>
              {headerGroup.headers.map((header) => {
                const canSort = header.column.getCanSort();
                const sortDir = header.column.getIsSorted();

                return (
                  <th
                    key={header.id}
                    scope="col"
                    aria-sort={
                      sortDir === "asc"
                        ? "ascending"
                        : sortDir === "desc"
                          ? "descending"
                          : undefined
                    }
                    tabIndex={canSort ? 0 : undefined}
                    className={cn(
                      "px-4 h-[34px] text-left text-[11.5px] font-medium text-text-muted",
                      "border-b border-border-default",
                      canSort && "cursor-pointer select-none hover:text-text-primary",
                    )}
                    onClick={canSort ? header.column.getToggleSortingHandler() : undefined}
                    onKeyDown={
                      canSort
                        ? (e) => {
                            if (e.key === "Enter" || e.key === " ") {
                              e.preventDefault();
                              header.column.toggleSorting();
                            }
                          }
                        : undefined
                    }
                  >
                    <div className="flex items-center gap-1.5">
                      {header.isPlaceholder
                        ? null
                        : flexRender(header.column.columnDef.header, header.getContext())}
                      {canSort && (
                        <span className="text-text-faint" aria-hidden="true">
                          {sortDir === "asc" ? (
                            <ArrowUp size={12} />
                          ) : sortDir === "desc" ? (
                            <ArrowDown size={12} />
                          ) : (
                            <ArrowUpDown size={12} />
                          )}
                        </span>
                      )}
                    </div>
                  </th>
                );
              })}
            </tr>
          ))}
        </thead>

        <tbody>
          {loading ? (
            Array.from({ length: 5 }, (_, i) => (
              <tr key={`skeleton-${i}`}>
                {columns.map((_, j) => (
                  <td
                    key={`skeleton-${i}-${j}`}
                    className="px-4 py-3 border-b border-border-default"
                  >
                    <div className="skel h-3.5" />
                  </td>
                ))}
              </tr>
            ))
          ) : table.getRowModel().rows.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className="px-4 py-12 text-center text-text-muted">
                {emptyMessage ?? t("noData")}
              </td>
            </tr>
          ) : (
            table.getRowModel().rows.map((row) => (
              <tr
                key={row.id}
                onClick={onRowClick ? () => handleRowClick(row.original) : undefined}
                onKeyDown={
                  onRowClick
                    ? (e) => {
                        if (e.key === "Enter") handleRowClick(row.original);
                      }
                    : undefined
                }
                tabIndex={onRowClick ? 0 : undefined}
                aria-selected={enableSelection ? row.getIsSelected() : undefined}
                className={cn(
                  "border-b border-border-default last:border-b-0 transition-colors duration-150",
                  onRowClick && "cursor-pointer hover:bg-bg-glass",
                  row.getIsSelected() && "bg-bg-glass-hover",
                )}
              >
                {/* No column-visibility feature registered, so every cell is visible. */}
                {row.getAllCells().map((cell) => (
                  <td key={cell.id} className={cn("px-4", compactMode ? "py-1.5" : "py-2.5")}>
                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

export { DataTable, type DataTableColumnDef, type DataTableProps };
