/**
 * SIFAP 2.0 — Dashboard principal
 * REQ-PAY-001 (BR-007): abertura de ciclo de pagamento
 * source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L42-L78
 */
import { PaymentCyclePanel } from "@/components/PaymentCyclePanel";
import { HealthBadge } from "@/components/HealthBadge";

export default function HomePage() {
  return (
    <main className="container mx-auto px-4 py-8 max-w-4xl">
      {/* Header */}
      <header className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-blue-800">SIFAP 2.0</h1>
          <p className="text-sm text-gray-500">
            Sistema de Fiscalização e Administração de Pagamentos
          </p>
        </div>
        <HealthBadge />
      </header>

      {/* Ciclo de Pagamento */}
      <section aria-labelledby="ciclo-heading" className="mb-8">
        <h2
          id="ciclo-heading"
          className="text-lg font-semibold text-gray-700 mb-4"
        >
          Ciclo de Pagamento Mensal
        </h2>
        <PaymentCyclePanel />
      </section>

      {/* Rastreabilidade */}
      <section
        aria-labelledby="trace-heading"
        className="rounded-lg border border-gray-200 bg-white p-4"
      >
        <h2
          id="trace-heading"
          className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-2"
        >
          Rastreabilidade Legado → Moderno
        </h2>
        <ul className="text-xs text-gray-600 space-y-1">
          <li>
            <span className="font-mono text-blue-700">REQ-PAY-001</span> ←{" "}
            <span className="font-mono">BATCHPGT.NSN#L42-L78</span> (BR-007:
            5º dia útil)
          </li>
          <li>
            <span className="font-mono text-blue-700">REQ-PAY-002</span> ←{" "}
            <span className="font-mono">BATCHPGT.NSN#L186-L255</span> (BR-001:
            fórmula BASE×FAT)
          </li>
          <li>
            <span className="font-mono text-blue-700">REQ-PAY-003</span> ←{" "}
            <span className="font-mono">BATCHPGT.NSN#L240-L248</span> (BR-013:
            HALF_UP)
          </li>
          <li>
            <span className="font-mono text-blue-700">REQ-PAY-011</span> ←{" "}
            <span className="font-mono">BATCHPGT.NSN#L92-L115</span> (BR-010:
            regime dezembro)
          </li>
        </ul>
      </section>
    </main>
  );
}
