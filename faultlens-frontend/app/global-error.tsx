"use client";

import "./globals.css";

/**
 * Replaces the root layout when it fails, so there is no i18n provider or theme script here:
 * the text is short and bilingual, shown in the default (dark) theme.
 */
export default function GlobalError({ error, retry }: { error: unknown; retry?: () => void }) {
  const digest = error instanceof Error ? (error as Error & { digest?: string }).digest : undefined;
  return (
    <html lang="tr" data-theme="dark">
      <body className="min-h-screen grid place-items-center p-6">
        <title>FaultLens</title>
        <main className="empty max-w-md" role="alert">
          <h1 className="h2 text-text-primary">FaultLens yüklenemedi · FaultLens failed to load</h1>
          <p className="small">
            Sayfayı yenileyin; sorun sürerse yöneticinize haber verin. · Reload the page; if it
            keeps happening, tell your admin.
          </p>
          {digest && <p className="mono xs t3">{digest}</p>}
          {retry && (
            <button type="button" className="btn mt-3" onClick={() => retry()}>
              Tekrar dene · Try again
            </button>
          )}
        </main>
      </body>
    </html>
  );
}
