"use client";

import { useRef, useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { motion, useInView } from "framer-motion";
import {
  ArrowRight,
  ChevronDown,
  Radio,
  Brain,
  Rocket,
  Hexagon,
  Server,
  Terminal,
  Container,
  Code,
} from "lucide-react";
import { Button } from "@/shared/components/ui/Button";
import { Card } from "@/shared/components/ui/Card";
import { LogRainCanvas } from "@/shared/components/canvas/LogRainCanvas";

/* ── Animated Counter ──────────────────────────────────────── */

function AnimatedCounter({
  end,
  suffix = "",
  prefix = "",
  duration = 2000,
}: {
  end: number;
  suffix?: string;
  prefix?: string;
  duration?: number;
}) {
  const [count, setCount] = useState(0);
  const ref = useRef<HTMLSpanElement>(null);
  const isInView = useInView(ref, { once: true });

  useEffect(() => {
    if (!isInView) return;

    let startTime: number;
    const animate = (timestamp: number) => {
      if (!startTime) startTime = timestamp;
      const progress = Math.min((timestamp - startTime) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setCount(Math.floor(eased * end));
      if (progress < 1) requestAnimationFrame(animate);
    };

    requestAnimationFrame(animate);
  }, [isInView, end, duration]);

  return (
    <span ref={ref} className="font-display font-bold text-4xl text-text-primary">
      {prefix}
      {count}
      {suffix}
    </span>
  );
}

/* ── Feature Card ──────────────────────────────────────────── */

function FeatureCard({
  icon,
  title,
  description,
  index,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
  index: number;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 30 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ delay: index * 0.15, duration: 0.5 }}
    >
      <Card
        variant="glass"
        hover
        glow
        className="h-full text-center"
        padding="lg"
      >
        <div className="w-14 h-14 rounded-2xl bg-accent/10 flex items-center justify-center mx-auto mb-4">
          {icon}
        </div>
        <h3 className="text-lg font-semibold font-display text-text-primary mb-2">
          {title}
        </h3>
        <p className="text-sm text-text-secondary leading-relaxed">
          {description}
        </p>
      </Card>
    </motion.div>
  );
}

/* ── Step Card ─────────────────────────────────────────────── */

function StepCard({
  number,
  title,
  description,
  icon,
  index,
}: {
  number: number;
  title: string;
  description: string;
  icon: React.ReactNode;
  index: number;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, x: index % 2 === 0 ? -20 : 20 }}
      whileInView={{ opacity: 1, x: 0 }}
      viewport={{ once: true }}
      transition={{ delay: index * 0.2, duration: 0.5 }}
      className="flex items-start gap-4"
    >
      <div className="shrink-0 w-12 h-12 rounded-2xl bg-accent/10 flex items-center justify-center">
        <span className="text-lg font-bold font-display text-accent">
          {number}
        </span>
      </div>
      <div>
        <div className="flex items-center gap-2 mb-1">
          {icon}
          <h3 className="font-semibold font-display text-text-primary">
            {title}
          </h3>
        </div>
        <p className="text-sm text-text-secondary leading-relaxed">
          {description}
        </p>
      </div>
    </motion.div>
  );
}

/* ── Landing Page ──────────────────────────────────────────── */

export default function LandingPage() {
  const scrollToFeatures = useCallback(() => {
    document.getElementById("features")?.scrollIntoView({ behavior: "smooth" });
  }, []);

  return (
    <div className="min-h-screen bg-bg-primary">
      {/* ── Hero Section ──────────────────────────── */}
      <section className="relative min-h-screen flex items-center justify-center overflow-hidden">
        {/* Canvas Background */}
        <div className="absolute inset-0 z-0">
          <LogRainCanvas className="absolute inset-0" />
        </div>

        {/* Grid Pattern */}
        <div className="absolute inset-0 bg-grid-pattern z-0" />

        {/* Glow Effect */}
        <div className="absolute inset-0 bg-accent-glow z-0" />

        {/* Content */}
        <div className="relative z-10 max-w-5xl mx-auto px-4 text-center">
          {/* Logo */}
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="flex items-center justify-center gap-3 mb-8"
          >
            <div className="w-12 h-12 rounded-2xl bg-accent/10 border border-accent/20 flex items-center justify-center">
              <Hexagon size={28} className="text-accent" />
            </div>
            <span className="text-3xl font-bold font-display text-text-primary">
              Log<span className="text-accent">Lens</span>
            </span>
          </motion.div>

          {/* Heading */}
          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2, duration: 0.6 }}
            className="text-4xl sm:text-5xl lg:text-6xl font-bold font-display text-text-primary leading-tight mb-6"
          >
            Your logs. Analyzed.{" "}
            <span className="text-accent">Instantly.</span>
          </motion.h1>

          {/* Subtitle */}
          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4, duration: 0.6 }}
            className="text-lg text-text-secondary max-w-2xl mx-auto mb-10 leading-relaxed"
          >
            Kubernetes, SSH ve Docker ortamlarınızdaki hataları gerçek zamanlı
            izleyin, kök nedenlerini saniyeler içinde bulun.
          </motion.p>

          {/* CTAs */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.6, duration: 0.6 }}
            className="flex items-center justify-center gap-4 flex-wrap"
          >
            <Link href="/login">
              <Button
                variant="primary"
                size="lg"
                icon={<ArrowRight size={18} />}
                iconPosition="right"
              >
                Hemen Başla
              </Button>
            </Link>
            <Button
              variant="secondary"
              size="lg"
              onClick={scrollToFeatures}
            >
              Nasıl Çalışır?
            </Button>
          </motion.div>

          {/* Scroll Indicator */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 1.2, duration: 0.6 }}
            className="mt-16"
          >
            <button
              onClick={scrollToFeatures}
              className="text-text-muted hover:text-text-secondary transition-colors"
              aria-label="Aşağı kaydır"
            >
              <ChevronDown size={24} className="animate-bounce" />
            </button>
          </motion.div>
        </div>
      </section>

      {/* ── Stats Section ─────────────────────────── */}
      <section className="py-20 border-y border-border-default">
        <div className="max-w-5xl mx-auto px-4">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-8 text-center">
            <div>
              <AnimatedCounter end={10} suffix="ms" prefix="<" />
              <p className="text-sm text-text-secondary mt-2">Analiz Süresi</p>
            </div>
            <div>
              <AnimatedCounter end={6} />
              <p className="text-sm text-text-secondary mt-2">
                Hata Kuralı Tanımlı
              </p>
            </div>
            <div>
              <AnimatedCounter end={3} />
              <p className="text-sm text-text-secondary mt-2">
                Platform Desteği (K8s + SSH + Docker)
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* ── Features Section ──────────────────────── */}
      <section id="features" className="py-20">
        <div className="max-w-5xl mx-auto px-4">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-12"
          >
            <h2 className="text-3xl font-bold font-display text-text-primary mb-3">
              Neden FaultLens?
            </h2>
            <p className="text-text-secondary max-w-xl mx-auto">
              Loglarınızı sadece izlemeyin — anlayın.
            </p>
          </motion.div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <FeatureCard
              icon={<Radio size={24} className="text-accent" />}
              title="Gerçek Zamanlı İzleme"
              description="WebSocket ile anlık log akışı. ERROR ve CRITICAL logları anında yakalayın."
              index={0}
            />
            <FeatureCard
              icon={<Brain size={24} className="text-accent" />}
              title="Kök Neden Analizi"
              description="Kural tabanlı ve AI destekli analiz ile hataların kök nedenini otomatik bulun."
              index={1}
            />
            <FeatureCard
              icon={<Rocket size={24} className="text-accent" />}
              title="Deployment Korelasyonu"
              description="Hataların hangi deployment'tan sonra başladığını otomatik tespit edin."
              index={2}
            />
          </div>
        </div>
      </section>

      {/* ── How It Works Section ──────────────────── */}
      <section className="py-20 bg-bg-secondary/50 border-y border-border-default">
        <div className="max-w-3xl mx-auto px-4">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-12"
          >
            <h2 className="text-3xl font-bold font-display text-text-primary mb-3">
              Nasıl Çalışır?
            </h2>
            <p className="text-text-secondary">3 adımda başlayın</p>
          </motion.div>

          <div className="space-y-8">
            <StepCard
              number={1}
              title="Kaynaklarınızı Bağlayın"
              description="Kubernetes cluster, SSH sunucu veya Docker container — kaynağınızı ekleyin."
              icon={<Server size={16} className="text-text-muted" />}
              index={0}
            />
            <StepCard
              number={2}
              title="Logları Toplayın"
              description="FaultLens gerçek zamanlı olarak loglarınızı toplar, gruplar ve severity'e göre sınıflandırır."
              icon={<Terminal size={16} className="text-text-muted" />}
              index={1}
            />
            <StepCard
              number={3}
              title="Analiz Edin"
              description="Otomatik kök neden analizi ve deployment korelasyonu ile hataları hızla çözün."
              icon={<Container size={16} className="text-text-muted" />}
              index={2}
            />
          </div>
        </div>
      </section>

      {/* ── Footer ────────────────────────────────── */}
      <footer className="py-12 border-t border-border-default">
        <div className="max-w-5xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <Hexagon size={18} className="text-accent" />
            <span className="font-display font-bold text-text-primary">
              Log<span className="text-accent">Lens</span>
            </span>
          </div>
          <p className="text-xs text-text-muted">
            Built with Spring Boot & Next.js
          </p>
          <a
            href="https://github.com"
            target="_blank"
            rel="noopener noreferrer"
            className="text-text-muted hover:text-text-primary transition-colors"
          >
            <Code size={20} />
          </a>
        </div>
      </footer>
    </div>
  );
}
