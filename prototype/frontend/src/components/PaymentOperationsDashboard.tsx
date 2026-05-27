"use client";

import { PaymentCyclePanel } from "@/components/PaymentCyclePanel";
import { useState } from "react";

type Role = "OPR" | "ADM" | "AUD";

interface PaymentDto {
  id: string;
  beneficiaryCpf: string;
  socialProgramCode: string;
  competence: string;
  grossAmount: number;
  netAmount: number;
  nominalDate: string;
  status: "G" | "P" | "E" | "C" | "R";
  bankCode: string;
  agency: string;
  account: string;
}

interface CycleDetailResponse {
  competence: string;
  paymentCount: number;
  totalAmount: number;
  payments: PaymentDto[];
}

interface ApprovalSummary {
  competence: string;
  filename: string;
  recordCount: number;
  totalAmount: number;
  content: string;
}

interface ReconciliationSummary {
  competence: string;
  duplicate: boolean;
  matched: number;
  divergences: number;
  conflicts: number;
}

interface AuditEventDto {
  id: string;
  actor: string;
  action: string;
  paymentId: string | null;
  prevState: string | null;
  newState: string | null;
  occurredAt: string;
  payload: string;
}

interface IpcaStatus {
  value: number | null;
  fetchedAt: string | null;
  cacheHit: boolean;
  fallback: boolean;
  alert: string | null;
}

const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export function PaymentOperationsDashboard() {
  const [competence, setCompetence] = useState(() => new Date().toISOString().slice(0, 7));
  const [role, setRole] = useState<Role>("OPR");
  const [actor, setActor] = useState("workshop-user");
  const [cycle, setCycle] = useState<CycleDetailResponse | null>(null);
  const [approval, setApproval] = useState<ApprovalSummary | null>(null);
  const [reconciliation, setReconciliation] = useState<ReconciliationSummary | null>(null);
  const [audit, setAudit] = useState<AuditEventDto[]>([]);
  const [ipca, setIpca] = useState<IpcaStatus | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function request<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(`${apiBase}${path}`, {
      ...init,
      headers: {
        "X-SIFAP-ACTOR": actor,
        "X-SIFAP-ROLE": role,
        ...(init?.headers ?? {}),
      },
    });
    const data = (await response.json()) as T;
    if (!response.ok) {
      const maybeError = data as { message?: string; code?: string };
      throw new Error(maybeError.message ?? maybeError.code ?? "Falha na chamada da API");
    }
    return data;
  }

  async function run<T>(operation: () => Promise<T>, onSuccess: (value: T) => void) {
    setLoading(true);
    setMessage(null);
    try {
      const value = await operation();
      onSuccess(value);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Erro inesperado");
    } finally {
      setLoading(false);
    }
  }

  function loadCycle() {
    void run(
      () => request<CycleDetailResponse>(`/api/v1/payment-cycles/${competence}`),
      setCycle,
    );
  }

  function approveCycle() {
    void run(
      () => request<ApprovalSummary>(`/api/v1/payment-cycles/${competence}/approve`, { method: "POST" }),
      (value) => {
        setApproval(value);
        loadCycle();
      },
    );
  }

  function importReturnFile(file: File | null) {
    if (!file) return;
    const formData = new FormData();
    formData.append("file", file);
    void run(
      () => request<ReconciliationSummary>(`/api/v1/payment-cycles/${competence}/reconcile`, {
        method: "POST",
        body: formData,
      }),
      (value) => {
        setReconciliation(value);
        loadCycle();
      },
    );
  }

  function loadAudit() {
    void run(() => request<AuditEventDto[]>("/api/v1/audit"), setAudit);
  }

  function loadIpca() {
    void run(() => request<IpcaStatus>("/api/v1/ipca/status"), setIpca);
  }

  return (
    <div className="space-y-6">
      <section className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <div className="grid gap-3 md:grid-cols-4">
          <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
            Competência
            <input
              type="month"
              value={competence}
              onChange={(event) => setCompetence(event.target.value)}
              className="rounded-md border border-gray-300 px-3 py-2 text-sm"
            />
          </label>
          <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
            Ator
            <input
              value={actor}
              onChange={(event) => setActor(event.target.value)}
              className="rounded-md border border-gray-300 px-3 py-2 text-sm"
            />
          </label>
          <label className="flex flex-col gap-1 text-sm font-medium text-gray-700">
            Papel
            <select
              value={role}
              onChange={(event) => setRole(event.target.value as Role)}
              className="rounded-md border border-gray-300 px-3 py-2 text-sm"
            >
              <option value="OPR">OPR</option>
              <option value="ADM">ADM</option>
              <option value="AUD">AUD</option>
            </select>
          </label>
          <button
            type="button"
            onClick={loadCycle}
            disabled={loading}
            className="self-end rounded-md bg-gray-900 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
          >
            Carregar Ciclo
          </button>
        </div>
        {message && <p className="mt-3 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-800">{message}</p>}
      </section>

      <PaymentCyclePanel />

      <section className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-gray-800">Pagamentos do Ciclo</h2>
            <p className="text-sm text-gray-500">
              {cycle ? `${cycle.paymentCount} pagamentos, total R$ ${cycle.totalAmount}` : "Carregue uma competência para ver os pagamentos."}
            </p>
          </div>
          <button
            type="button"
            onClick={approveCycle}
            disabled={loading}
            className="rounded-md bg-blue-700 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
          >
            Aprovar e Gerar CNAB
          </button>
        </div>

        <div className="mt-4 overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="border-b border-gray-200 text-xs uppercase text-gray-500">
              <tr>
                <th className="px-2 py-2">CPF</th>
                <th className="px-2 py-2">Programa</th>
                <th className="px-2 py-2">Status</th>
                <th className="px-2 py-2">Valor Líquido</th>
                <th className="px-2 py-2">Banco</th>
                <th className="px-2 py-2">Data Nominal</th>
              </tr>
            </thead>
            <tbody>
              {(cycle?.payments ?? []).map((payment) => (
                <tr key={payment.id} className="border-b border-gray-100">
                  <td className="px-2 py-2 font-mono">{payment.beneficiaryCpf}</td>
                  <td className="px-2 py-2">{payment.socialProgramCode}</td>
                  <td className="px-2 py-2"><PaymentStatusBadge status={payment.status} /></td>
                  <td className="px-2 py-2">R$ {payment.netAmount}</td>
                  <td className="px-2 py-2 font-mono">{payment.bankCode}/{payment.agency}/{payment.account}</td>
                  <td className="px-2 py-2">{payment.nominalDate}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {approval && (
        <section className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-gray-800">Arquivo CNAB 240</h2>
              <p className="text-sm text-gray-500">{approval.filename} — {approval.recordCount} registros — R$ {approval.totalAmount}</p>
            </div>
            <label className="rounded-md border border-gray-300 px-4 py-2 text-sm font-semibold text-gray-700">
              Importar Retorno
              <input
                type="file"
                accept=".txt,.ret,.rem"
                className="sr-only"
                onChange={(event) => importReturnFile(event.target.files?.item(0) ?? null)}
              />
            </label>
          </div>
          <textarea
            readOnly
            value={approval.content}
            className="mt-3 h-40 w-full rounded-md border border-gray-200 bg-gray-50 p-3 font-mono text-xs"
          />
        </section>
      )}

      {reconciliation && (
        <section className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-800">Resultado da Conciliação</h2>
          <div className="mt-3 grid gap-3 sm:grid-cols-4">
            <Metric label="Duplicado" value={reconciliation.duplicate ? "sim" : "não"} />
            <Metric label="Conciliados" value={String(reconciliation.matched)} />
            <Metric label="Divergências" value={String(reconciliation.divergences)} />
            <Metric label="Conflitos" value={String(reconciliation.conflicts)} />
          </div>
        </section>
      )}

      <section className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-gray-800">IPCA BCB/SGS 433</h2>
            <p className="text-sm text-gray-500">
              {ipca
                ? `Valor ${ipca.value ?? "indisponível"} via ${ipca.cacheHit ? "cache" : "BCB"}`
                : "Consulte a série oficial com cache de 24h."}
            </p>
          </div>
          <button type="button" onClick={loadIpca} className="rounded-md border border-gray-300 px-4 py-2 text-sm font-semibold text-gray-700">
            Consultar IPCA
          </button>
        </div>
        {ipca?.alert && <p className="mt-3 rounded-md border border-yellow-200 bg-yellow-50 p-3 text-sm text-yellow-800">{ipca.alert}</p>}
      </section>

      <section className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-gray-800">Trilha de Auditoria</h2>
          <button type="button" onClick={loadAudit} className="rounded-md border border-gray-300 px-4 py-2 text-sm font-semibold text-gray-700">
            Atualizar Auditoria
          </button>
        </div>
        <div className="mt-4 max-h-72 overflow-auto">
          <table className="min-w-full text-left text-xs">
            <thead className="border-b border-gray-200 uppercase text-gray-500">
              <tr>
                <th className="px-2 py-2">Quando</th>
                <th className="px-2 py-2">Ação</th>
                <th className="px-2 py-2">Ator</th>
                <th className="px-2 py-2">Estado</th>
                <th className="px-2 py-2">Payload</th>
              </tr>
            </thead>
            <tbody>
              {audit.map((event) => (
                <tr key={event.id} className="border-b border-gray-100">
                  <td className="px-2 py-2">{new Date(event.occurredAt).toLocaleString("pt-BR")}</td>
                  <td className="px-2 py-2 font-mono">{event.action}</td>
                  <td className="px-2 py-2">{event.actor}</td>
                  <td className="px-2 py-2">{event.prevState ?? "-"} → {event.newState ?? "-"}</td>
                  <td className="px-2 py-2 font-mono">{event.payload}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

function PaymentStatusBadge({ status }: { status: PaymentDto["status"] }) {
  const classes: Record<PaymentDto["status"], string> = {
    G: "bg-gray-100 text-gray-700",
    P: "bg-yellow-100 text-yellow-800",
    E: "bg-blue-100 text-blue-800",
    C: "bg-green-100 text-green-800",
    R: "bg-red-100 text-red-800",
  };
  return <span className={`rounded px-2 py-1 text-xs font-semibold ${classes[status]}`}>{status}</span>;
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-gray-200 p-3">
      <p className="text-xs uppercase text-gray-500">{label}</p>
      <p className="mt-1 text-xl font-semibold text-gray-900">{value}</p>
    </div>
  );
}