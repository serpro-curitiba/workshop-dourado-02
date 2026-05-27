package br.gov.serpro.sifap.shared.security;

public record DevActor(String actor, Role role) {
    public boolean hasRole(Role expected) {
        return role == expected;
    }
}