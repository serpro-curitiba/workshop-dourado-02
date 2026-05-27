"use client";

/**
 * Exibe o status do backend via /actuator/health.
 * Sem dados sensíveis — apenas status UP/DOWN.
 */
import { useEffect, useState } from "react";

type Status = "UP" | "DOWN" | "loading";

export function HealthBadge() {
  const [status, setStatus] = useState<Status>("loading");

  useEffect(() => {
    const apiBase =
      process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
    fetch(`${apiBase}/actuator/health`, { cache: "no-store" })
      .then((r) => r.json())
      .then((d) => setStatus(d.status === "UP" ? "UP" : "DOWN"))
      .catch(() => setStatus("DOWN"));
  }, []);

  const color =
    status === "UP"
      ? "bg-green-100 text-green-800 border-green-300"
      : status === "DOWN"
        ? "bg-red-100 text-red-800 border-red-300"
        : "bg-gray-100 text-gray-500 border-gray-200";

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium ${color}`}
      aria-live="polite"
      aria-label={`Backend status: ${status}`}
    >
      <span
        className={`h-2 w-2 rounded-full ${status === "UP" ? "bg-green-500" : status === "DOWN" ? "bg-red-500" : "bg-gray-400"}`}
        aria-hidden="true"
      />
      Backend {status}
    </span>
  );
}
