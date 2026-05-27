package br.gov.serpro.sifap.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class DevActorResolver {

    public DevActor resolve(HttpServletRequest request) {
        String actor = headerOrDefault(request, "X-SIFAP-ACTOR", "workshop-user");
        String roleValue = headerOrDefault(request, "X-SIFAP-ROLE", "OPR");
        Role role = parseRole(roleValue);
        return new DevActor(actor, role);
    }

    private static String headerOrDefault(HttpServletRequest request, String name, String fallback) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static Role parseRole(String value) {
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Role.OPR;
        }
    }
}