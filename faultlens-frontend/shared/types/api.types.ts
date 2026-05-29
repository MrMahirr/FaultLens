/**
 * Generic API response types.
 * Backend'den gelen tüm yanıtlar bu yapılarla sarmalanır.
 */

/** Single item API response wrapper */
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  timestamp: string;
}

/** Paginated API response wrapper — backend PagedResponse.java ile birebir uyumlu */
export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

/* ── Pagination Helpers ──────────────────────────────────────── */

/** Backend PagedResponse'dan hesaplanabilir sayfa kontrolleri */
export function hasNextPage(paged: PagedResponse<unknown>): boolean {
  return paged.page + 1 < paged.totalPages;
}

export function hasPreviousPage(paged: PagedResponse<unknown>): boolean {
  return paged.page > 0;
}

/** API error response structure — backend ErrorEnvelope ile uyumlu */
export interface ApiError {
  success: false;
  error: {
    code: string;
    message: string;
    timestamp: string;
  };
}

/** Pagination request parameters */
export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
  direction?: "ASC" | "DESC";
}

/** Date range filter */
export interface DateRange {
  startDate?: string;
  endDate?: string;
}
