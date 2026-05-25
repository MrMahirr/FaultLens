"use client";

import { useRef, useEffect } from "react";
import { useTheme } from "@/shared/hooks/useTheme";

/* ── Log Lines ─────────────────────────────────────────────── */

const LOG_LINES = [
  "ERROR  NullPointerException at PaymentService.java:142",
  "WARN   Connection timeout: db-primary:5432",
  "ERROR  OutOfMemoryError: Java heap space",
  "INFO   Pod payment-service-7d9f restarted",
  "CRITICAL  Too many connections to database",
  "ERROR  TLS handshake failed: certificate expired",
  "WARN   Slow query detected: SELECT * FROM orders (2340ms)",
  "INFO   Deployment v2.3.1 rolled out to 3/3 pods",
  "ERROR  Failed to pull image: unauthorized",
  "WARN   Memory usage at 85% on worker-3",
  "ERROR  Connection refused: redis-cluster:6379",
  "INFO   Health check passed: 200 OK (12ms)",
  "CRITICAL  Disk usage at 95% — immediate action required",
  "ERROR  Stack overflow in RecursiveProcessor.java:78",
  "WARN   Request rate limit exceeded: 429 Too Many Requests",
  "INFO   Auto-scaling triggered: replicas 3 → 5",
  "ERROR  Deadlock detected in TransactionManager",
  "WARN   Certificate expires in 7 days",
  "ERROR  ClassNotFoundException: com.payment.legacy.Handler",
  "INFO   Backup completed successfully in 45s",
];

/* ── Types ─────────────────────────────────────────────────── */

interface FloatingLine {
  text: string;
  x: number;
  y: number;
  speed: number;
  opacity: number;
  fontSize: number;
}

interface LogRainCanvasProps {
  className?: string;
  reducedOpacity?: boolean;
}

/* ── Component ─────────────────────────────────────────────── */

function LogRainCanvas({ className, reducedOpacity = false }: LogRainCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const { isDark } = useTheme();
  const linesRef = useRef<FloatingLine[]>([]);
  const animationRef = useRef<number>(0);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const resizeCanvas = () => {
      const dpr = window.devicePixelRatio || 1;
      canvas.width = canvas.offsetWidth * dpr;
      canvas.height = canvas.offsetHeight * dpr;
      ctx.scale(dpr, dpr);
    };

    resizeCanvas();
    window.addEventListener("resize", resizeCanvas);

    // Initialize lines
    const baseOpacity = reducedOpacity ? 0.02 : isDark ? 0.05 : 0.03;
    const lineCount = Math.min(30, Math.floor(canvas.offsetWidth / 60));

    linesRef.current = Array.from({ length: lineCount }, () => ({
      text: LOG_LINES[Math.floor(Math.random() * LOG_LINES.length)],
      x: Math.random() * canvas.offsetWidth,
      y: Math.random() * canvas.offsetHeight,
      speed: 0.3 + Math.random() * 0.8,
      opacity: baseOpacity + Math.random() * baseOpacity,
      fontSize: 10 + Math.random() * 3,
    }));

    const animate = () => {
      ctx.clearRect(0, 0, canvas.offsetWidth, canvas.offsetHeight);

      const textColor = isDark ? "255, 255, 255" : "0, 0, 0";

      for (const line of linesRef.current) {
        ctx.font = `${line.fontSize}px "JetBrains Mono", monospace`;
        ctx.fillStyle = `rgba(${textColor}, ${line.opacity})`;
        ctx.fillText(line.text, line.x, line.y);

        // Move line
        line.y += line.speed;

        // Reset when off screen
        if (line.y > canvas.offsetHeight + 20) {
          line.y = -20;
          line.x = Math.random() * canvas.offsetWidth;
          line.text = LOG_LINES[Math.floor(Math.random() * LOG_LINES.length)];
        }
      }

      animationRef.current = requestAnimationFrame(animate);
    };

    animationRef.current = requestAnimationFrame(animate);

    return () => {
      cancelAnimationFrame(animationRef.current);
      window.removeEventListener("resize", resizeCanvas);
    };
  }, [isDark, reducedOpacity]);

  return (
    <canvas
      ref={canvasRef}
      className={className}
      style={{ width: "100%", height: "100%" }}
    />
  );
}

export { LogRainCanvas, type LogRainCanvasProps };
