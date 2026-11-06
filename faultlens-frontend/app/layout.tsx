import type { Metadata } from "next";
import { Inter, JetBrains_Mono } from "next/font/google";
import { QueryProvider } from "@/shared/components/providers/QueryProvider";
import { ThemeProvider } from "@/shared/components/providers/ThemeProvider";
import { AppToaster } from "@/shared/components/feedback/AppToaster";
import { THEME_BOOTSTRAP_SCRIPT } from "@/shared/lib/theme";
import "./globals.css";

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin", "latin-ext"],
  display: "swap",
});

const jetbrainsMono = JetBrains_Mono({
  variable: "--font-jetbrains",
  subsets: ["latin", "latin-ext"],
  display: "swap",
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

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="tr"
      data-theme="dark"
      data-motion="on"
      className={`${inter.variable} ${jetbrainsMono.variable}`}
      suppressHydrationWarning
    >
      <head>
        {/* Applies the stored theme/motion before first paint (no light/dark flash). */}
        <script dangerouslySetInnerHTML={{ __html: THEME_BOOTSTRAP_SCRIPT }} />
      </head>
      <body className="min-h-screen bg-bg-primary text-text-primary font-body antialiased">
        <QueryProvider>
          <ThemeProvider>
            {children}
            <AppToaster />
          </ThemeProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
