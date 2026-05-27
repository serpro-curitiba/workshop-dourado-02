package br.gov.serpro.sifap.conciliacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Arquivo de retorno bancario (CNAB 240) ja importado.
 *
 * O hash SHA-256 do conteudo serve como chave natural de idempotencia:
 * o segundo upload do mesmo arquivo NAO reprocessa registros.
 *
 * REQ-PAY-008 / BR-022 (GREENFIELD): resolve loop operacional descoberto
 * na arqueologia onde BATCHCON processava o mesmo arquivo 2x se o
 * operador reexecutasse o job.
 */
@Entity
@Table(name = "bank_return_file")
public class BankReturnFile {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String sha256;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "imported_at", nullable = false)
    private OffsetDateTime importedAt;

    protected BankReturnFile() {
    }

    public BankReturnFile(String sha256, String filename, long sizeBytes) {
        this.id = UUID.randomUUID();
        this.sha256 = sha256;
        this.filename = filename;
        this.sizeBytes = sizeBytes;
        this.importedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getSha256() {
        return sha256;
    }

    public String getFilename() {
        return filename;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public OffsetDateTime getImportedAt() {
        return importedAt;
    }
}
