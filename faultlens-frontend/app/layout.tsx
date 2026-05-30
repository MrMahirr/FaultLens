import type { Metadata } from "next";
import { Space_Grotesk, Inter, JetBrains_Mono } from "next/font/google";
import { QueryProvider } from "@/shared/components/providers/QueryProvider";
import { ThemeProvider } from "@/shared/components/providers/ThemeProvider";
import "./globals.css";

const spaceGrotesk = Space_Grotesk({
  variable: "--font-display",
  subsets: ["latin"],
  display: "swap",
  weight: ["300", "400", "500", "600", "700"],
});

const inter = Inter({
  variable: "--font-body",
  subsets: ["latin"],
  display: "swap",
  weight: ["300", "400", "500", "600", "700"],
});

const jetbrainsMono = JetBrains_Mono({
  variable: "--font-mono",
  subsets: ["latin"],
  display: "swap",
  weight: ["300", "400", "500", "600", "700"],
});

export const metadata: Metadata = {
  title: "FaultLens — Real-time Log Analysis Platform",
  description:
    "Kubernetes, SSH ve Docker ortamlarındaki uzak sunucu loglarını gerçek zamanlı izleyen, hata analizi yapan ve deployment korelasyonu sunan log analiz platformu.",
  keywords: [
    "log analysis",
    "kubernetes",
    "monitoring",
    "error tracking",
    "deployment correlation",
  ],
};

import { Toaster } from "react-hot-toast";

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="tr"
      data-theme="dark"
      className={`${spaceGrotesk.variable} ${inter.variable} ${jetbrainsMono.variable}`}
      suppressHydrationWarning
    >
      <body className="min-h-screen bg-bg-primary text-text-primary font-body antialiased">
        <QueryProvider>
          <ThemeProvider>
            {children}
            <Toaster
              position="bottom-right"
              toastOptions={{
                className: "!bg-bg-tertiary !text-text-primary !border !border-border-default !rounded-xl !shadow-lg",
                success: {
                  iconTheme: {
                    primary: "var(--color-success)",
                    secondary: "var(--color-bg-tertiary)",
                  },
                },
                error: {
                  iconTheme: {
                    primary: "var(--color-error)",
                    secondary: "var(--color-bg-tertiary)",
                  },
                },
              }}
            />
          </ThemeProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
