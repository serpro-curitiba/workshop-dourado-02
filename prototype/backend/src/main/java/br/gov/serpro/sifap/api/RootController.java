package br.gov.serpro.sifap.api;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {
    @GetMapping("/")
    public Map<String, Object> index() {
        return Map.of(
                "service", "SIFAP 2.0 Backend",
                "status", "UP",
                "health", "/actuator/health",
                "apis", Map.of("paymentCycles", "/api/v1/payment-cycles"));
    }
}
