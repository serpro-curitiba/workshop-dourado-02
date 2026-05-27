package br.gov.serpro.sifap.conciliacao.application;

import br.gov.serpro.sifap.conciliacao.domain.BankReturnFile;
import br.gov.serpro.sifap.conciliacao.domain.BankReturnFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Garante idempotencia da importacao de arquivos CNAB 240.
 *
 * REQ-PAY-008 / BR-022 (GREENFIELD): o legado BATCHCON nao tinha controle
 * de hash, permitindo reprocessamento se o operador rodasse novamente.
 * Aqui calculamos SHA-256 do conteudo e usamos UNIQUE constraint da tabela
 * bank_return_file como guard.
 */
@Service
public class FileIdempotencyService {

    private final BankReturnFileRepository repository;

    public FileIdempotencyService(BankReturnFileRepository repository) {
        this.repository = repository;
    }

    /**
     * Registra o arquivo se ainda nao existe; retorna {@code duplicate=true}
     * se o mesmo SHA-256 ja foi importado anteriormente.
     */
    @Transactional
    public ImportResult register(byte[] content, String filename) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("conteudo do arquivo nao pode ser vazio");
        }
        String sha256 = sha256Hex(content);
        return repository.findBySha256(sha256)
                .map(existing -> new ImportResult(existing, true))
                .orElseGet(() -> {
                    BankReturnFile saved = repository.save(
                            new BankReturnFile(sha256, filename, content.length));
                    return new ImportResult(saved, false);
                });
    }

    static String sha256Hex(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nao disponivel na JVM", e);
        }
    }

    public record ImportResult(BankReturnFile file, boolean duplicate) {
    }
}
