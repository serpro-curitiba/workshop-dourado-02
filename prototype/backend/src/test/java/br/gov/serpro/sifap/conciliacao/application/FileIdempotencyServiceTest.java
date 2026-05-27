package br.gov.serpro.sifap.conciliacao.application;

import br.gov.serpro.sifap.conciliacao.domain.BankReturnFileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de idempotencia de importacao CNAB.
 * REQ-PAY-008 / BR-022: segundo upload do mesmo arquivo retorna duplicate=true
 * e nao cria registros adicionais.
 */
@DataJpaTest
@AutoConfigureTestDatabase
@Import(FileIdempotencyService.class)
class FileIdempotencyServiceTest {

    @Autowired
    private FileIdempotencyService service;

    @Autowired
    private BankReturnFileRepository repository;

    // requirement: REQ-PAY-008
    @Test
    void firstUploadIsPersistedAndNotDuplicate() {
        byte[] content = "CNAB-CONTENT-001".getBytes(StandardCharsets.UTF_8);
        FileIdempotencyService.ImportResult r = service.register(content, "retorno-001.ret");

        assertFalse(r.duplicate());
        assertNotNull(r.file().getId());
        assertEquals(64, r.file().getSha256().length());
        assertEquals(1L, repository.count());
    }

    // requirement: REQ-PAY-008
    @Test
    void secondUploadOfSameContentReturnsDuplicateWithoutNewRow() {
        byte[] content = "CNAB-CONTENT-DUP".getBytes(StandardCharsets.UTF_8);

        FileIdempotencyService.ImportResult first = service.register(content, "a.ret");
        FileIdempotencyService.ImportResult second = service.register(content, "b.ret");

        assertFalse(first.duplicate());
        assertTrue(second.duplicate(), "segundo upload do mesmo SHA-256 deve sinalizar duplicate");
        assertEquals(first.file().getId(), second.file().getId(),
                "deve retornar referencia ao registro original");
        assertEquals(1L, repository.count(), "tabela nao deve crescer no reupload");
    }

    // requirement: REQ-PAY-008
    @Test
    void differentContentProducesDistinctRows() {
        service.register("AAA".getBytes(StandardCharsets.UTF_8), "a.ret");
        service.register("BBB".getBytes(StandardCharsets.UTF_8), "b.ret");

        assertEquals(2L, repository.count());
    }

    // requirement: REQ-PAY-008
    @Test
    void emptyContentIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register(new byte[0], "vazio.ret"));
        assertThrows(IllegalArgumentException.class,
                () -> service.register(null, "null.ret"));
    }
}
