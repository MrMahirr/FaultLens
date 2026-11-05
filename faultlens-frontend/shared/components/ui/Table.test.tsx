import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { DataTable, type DataTableColumnDef } from "./Table";

interface Row {
  name: string;
  count: number;
  at: string;
}

const rows: Row[] = [
  { name: "beta", count: 10, at: "2026-11-02T10:00:00Z" },
  { name: "alpha", count: 2, at: "2026-11-04T10:00:00Z" },
  { name: "gamma", count: 33, at: "2026-11-01T10:00:00Z" },
];

const columns: DataTableColumnDef<Row>[] = [
  { accessorKey: "name", header: "Ad" },
  { accessorKey: "count", header: "Adet" },
  { accessorKey: "at", header: "Zaman", enableSorting: false },
];

/** First-column text of each body row, top to bottom. */
function firstColumn() {
  const body = screen.getAllByRole("rowgroup")[1];
  return within(body)
    .getAllByRole("row")
    .map((row) => within(row).getAllByRole("cell")[0].textContent);
}

describe("DataTable", () => {
  it("renders headers and rows in data order", () => {
    render(<DataTable data={rows} columns={columns} />);

    expect(screen.getAllByRole("columnheader").map((th) => th.textContent)).toEqual([
      "Ad",
      "Adet",
      "Zaman",
    ]);
    expect(firstColumn()).toEqual(["beta", "alpha", "gamma"]);
  });

  it("sorts text columns ascending, then descending, on header click", async () => {
    const user = userEvent.setup();
    render(<DataTable data={rows} columns={columns} />);

    await user.click(screen.getByText("Ad"));
    expect(firstColumn()).toEqual(["alpha", "beta", "gamma"]);

    await user.click(screen.getByText("Ad"));
    expect(firstColumn()).toEqual(["gamma", "beta", "alpha"]);
  });

  it("sorts number columns numerically (descending first, like v8's auto sort)", async () => {
    const user = userEvent.setup();
    render(<DataTable data={rows} columns={columns} />);

    await user.click(screen.getByText("Adet"));
    expect(firstColumn()).toEqual(["gamma", "beta", "alpha"]); // 33, 10, 2

    await user.click(screen.getByText("Adet"));
    expect(firstColumn()).toEqual(["alpha", "beta", "gamma"]); // 2, 10, 33
  });

  it("ignores clicks on columns with sorting disabled", async () => {
    const user = userEvent.setup();
    render(<DataTable data={rows} columns={columns} />);

    await user.click(screen.getByText("Zaman"));

    expect(firstColumn()).toEqual(["beta", "alpha", "gamma"]);
  });

  it("disables sorting entirely with enableSorting={false}", async () => {
    const user = userEvent.setup();
    render(<DataTable data={rows} columns={columns} enableSorting={false} />);

    await user.click(screen.getByText("Ad"));

    expect(firstColumn()).toEqual(["beta", "alpha", "gamma"]);
  });

  it("passes the original row to onRowClick", async () => {
    const user = userEvent.setup();
    const onRowClick = vi.fn();
    render(<DataTable data={rows} columns={columns} onRowClick={onRowClick} />);

    await user.click(screen.getByText("gamma"));

    expect(onRowClick).toHaveBeenCalledWith(rows[2]);
  });

  it("shows the empty message without data and skeleton rows while loading", () => {
    const { rerender } = render(<DataTable data={[]} columns={columns} emptyMessage="Kayıt yok" />);
    expect(screen.getByText("Kayıt yok")).toBeInTheDocument();

    rerender(<DataTable data={[]} columns={columns} loading />);
    expect(screen.queryByText("Kayıt yok")).not.toBeInTheDocument();
    expect(screen.getAllByRole("row")).toHaveLength(1 + 5); // header + 5 skeleton rows
  });
});
