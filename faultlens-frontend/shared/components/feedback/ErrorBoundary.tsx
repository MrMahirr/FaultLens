"use client";

import { Component, type ReactNode, type ErrorInfo } from "react";
import { AlertTriangle, RefreshCw } from "lucide-react";
import { Button } from "@/shared/components/ui/Button";

/* ── Types ─────────────────────────────────────────────────── */

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

/* ── Component ─────────────────────────────────────────────── */

class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    this.props.onError?.(error, errorInfo);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="flex flex-col items-center justify-center py-16 px-4 text-center">
          <div className="flex items-center justify-center w-16 h-16 rounded-2xl bg-error/10 text-error mb-4">
            <AlertTriangle size={28} />
          </div>

          <h3 className="text-lg font-semibold font-display text-text-primary mb-1">
            Bir hata oluştu
          </h3>

          <p className="text-sm text-text-secondary max-w-sm mb-2">
            Beklenmeyen bir hata meydana geldi. Lütfen sayfayı yenileyin.
          </p>

          {this.state.error && (
            <p className="text-xs font-mono text-text-muted bg-bg-tertiary rounded-lg px-3 py-2 max-w-md mb-4 break-all">
              {this.state.error.message}
            </p>
          )}

          <Button
            variant="secondary"
            size="sm"
            icon={<RefreshCw size={14} />}
            onClick={this.handleReset}
          >
            Tekrar Dene
          </Button>
        </div>
      );
    }

    return this.props.children;
  }
}

export { ErrorBoundary, type ErrorBoundaryProps };
