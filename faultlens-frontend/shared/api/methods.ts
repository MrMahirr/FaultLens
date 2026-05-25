/**
 * HTTP Method enumeration.
 * Hiçbir yerde string literal kullanılmaz — sadece HttpMethod.* ile erişilir.
 */
export enum HttpMethod {
  GET = "GET",
  POST = "POST",
  PUT = "PUT",
  PATCH = "PATCH",
  DELETE = "DELETE",
}
