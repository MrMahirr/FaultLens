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

/** Paginated API response wrapper */
export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

/** API error response structure */
export interface ApiError {
  success: false;
  message: string;
  errorCode?: string;
  details?: Record<string, string[]>;
  timestamp: string;
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
