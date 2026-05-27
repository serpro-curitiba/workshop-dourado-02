package br.gov.serpro.sifap.calculo.api;

import br.gov.serpro.sifap.calculo.application.IpcaProviderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ipca")
public class IpcaController {

    private final IpcaProviderService service;

    public IpcaController(IpcaProviderService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public IpcaProviderService.IpcaStatus status() {
        return service.latestStatus();
    }
}