package br.gov.serpro.sifap.pagamento.domain;

/**
 * Maquina de estados do Pagamento (REQ-PAY-006).
 *
 * G = Gerado            (apos batch de geracao)
 * P = Pendente Aprovacao
 * E = Enviado ao banco  (apos approve + CNAB export)
 * C = Conciliado
 * R = Retornado/Rejeitado pelo banco
 *
 * source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L320-L380
 */
public enum PaymentStatus {
    G, P, E, C, R
}
