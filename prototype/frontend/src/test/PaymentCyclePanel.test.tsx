/**
 * Testes do PaymentCyclePanel
 * REQ-PAY-001 (BR-007): abertura de pagamentos
 * source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L42-L78
 */
import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { PaymentCyclePanel } from "../components/PaymentCyclePanel";

const mockFetch = vi.fn();
vi.stubGlobal("fetch", mockFetch);

beforeEach(() => {
  mockFetch.mockReset();
});

describe("PaymentCyclePanel", () => {
  it("renderiza o formulário com campo de competência", () => {
    render(<PaymentCyclePanel />);
    expect(screen.getByLabelText(/competência/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /abrir pagamentos/i })).toBeInTheDocument();
  });

  it("exibe sucesso quando API retorna 201", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        competence: "2026-06",
        nominalDate: "2026-06-05",
        generatedCount: 0,
      }),
    });

    render(<PaymentCyclePanel />);
    fireEvent.click(screen.getByRole("button", { name: /abrir pagamentos/i }));

    await waitFor(() =>
      expect(screen.getByText(/pagamentos abertos com sucesso/i)).toBeInTheDocument()
    );
    expect(screen.getByText("2026-06")).toBeInTheDocument();
  });

  it("exibe CYCLE_TOO_EARLY quando API retorna 422", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      json: async () => ({
        code: "CYCLE_TOO_EARLY",
        message: "Pagamentos da competencia 2026-06 so podem ser abertos a partir de 2026-06-05",
      }),
    });

    render(<PaymentCyclePanel />);
    fireEvent.click(screen.getByRole("button", { name: /abrir pagamentos/i }));

    await waitFor(() =>
      expect(screen.getByText("CYCLE_TOO_EARLY")).toBeInTheDocument()
    );
  });

  it("exibe CYCLE_ALREADY_EXISTS quando API retorna 409", async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      json: async () => ({
        code: "CYCLE_ALREADY_EXISTS",
        message: "Pagamentos ja existem para competencia 2026-06",
      }),
    });

    render(<PaymentCyclePanel />);
    fireEvent.click(screen.getByRole("button", { name: /abrir pagamentos/i }));

    await waitFor(() =>
      expect(screen.getByText("CYCLE_ALREADY_EXISTS")).toBeInTheDocument()
    );
  });

  it("exibe NETWORK_ERROR quando fetch falha", async () => {
    mockFetch.mockRejectedValueOnce(new Error("network"));

    render(<PaymentCyclePanel />);
    fireEvent.click(screen.getByRole("button", { name: /abrir pagamentos/i }));

    await waitFor(() =>
      expect(screen.getByText("NETWORK_ERROR")).toBeInTheDocument()
    );
  });
});
