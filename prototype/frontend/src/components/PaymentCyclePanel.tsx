"use client";

/**
 * Painel de abertura de pagamentos da competencia.
 *
 * REQ-PAY-001 (BR-007): POST /api/v1/payment-cycles no 5º dia útil.
 * source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L42-L78
 */

import { useState } from "react";

type CycleResult =
  | { ok: true; competence: string; nominalDate: string; generatedCount: number }
  | { ok: false; code: string; message: string };

export function PaymentCyclePanel() {
  const [competence, setCompetence] = useState(
    () => new Date().toISOString().slice(0, 7), // YYYY-MM
  );
  const [result, setResult] = useState<CycleResult | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setResult(null);
    try {
      const apiBase =
        process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
      const res = await fetch(`${apiBase}/api/v1/payment-cycles`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ competence }),
      });
      const data = await res.json();
      if (res.ok) {
        setResult({
          ok: true,
          competence: data.competence,
          nominalDate: data.nominalDate,
          generatedCount: data.generatedCount,
        });
      } else {
        setResult({ ok: false, code: data.code, message: data.message });
      }
    } catch {
      setResult({ ok: false, code: "NETWORK_ERROR", message: "Sem conexão com o backend." });
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4 sm:flex-row sm:items-end">
        <div className="flex flex-col gap-1">
          <label
            htmlFor="competence"
            className="text-sm font-medium text-gray-700"
          >
            Competência (YYYY-MM)
          </label>
          <input
            id="competence"
            type="month"
            value={competence}
            onChange={(e) => setCompetence(e.target.value)}
            required
            className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            aria-describedby="competence-hint"
          />
          <span id="competence-hint" className="text-xs text-gray-400">
            Pagamentos disponiveis a partir do 5º dia util do mes (BR-007)
          </span>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="rounded-md bg-blue-700 px-5 py-2 text-sm font-semibold text-white hover:bg-blue-800 disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          aria-busy={loading}
        >
          {loading ? "Abrindo..." : "Abrir Pagamentos"}
        </button>
      </form>

      {result && (
        <div
          role="alert"
          className={`mt-4 rounded-md p-4 text-sm ${
            result.ok
              ? "bg-green-50 text-green-800 border border-green-200"
              : "bg-red-50 text-red-800 border border-red-200"
          }`}
        >
          {result.ok ? (
            <>
              <p className="font-semibold">Pagamentos abertos com sucesso</p>
              <ul className="mt-1 space-y-0.5 text-xs">
                <li>Competência: <strong>{result.competence}</strong></li>
                <li>Data nominal: <strong>{result.nominalDate}</strong></li>
                <li>Pagamentos gerados: <strong>{result.generatedCount}</strong></li>
              </ul>
            </>
          ) : (
            <>
              <p className="font-semibold">{result.code}</p>
              <p className="text-xs mt-1">{result.message}</p>
            </>
          )}
        </div>
      )}
    </div>
  );
}
