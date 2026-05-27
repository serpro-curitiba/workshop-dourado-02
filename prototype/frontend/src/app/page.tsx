/**
 * SIFAP 2.0 — Dashboard principal
 * REQ-PAY-001..014: operacao do ciclo de pagamento
 * source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L42-L78
 */

import { HealthBadge } from "@/components/HealthBadge";
import { PaymentOperationsDashboard } from "@/components/PaymentOperationsDashboard";

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

      <section aria-labelledby="ciclo-heading" className="mb-8">
        <h2
          id="ciclo-heading"
          className="text-lg font-semibold text-gray-700 mb-4"
        >
          Ciclo de Pagamento Mensal
        </h2>
        <PaymentOperationsDashboard />
      </section>
    </main>
  );
}
