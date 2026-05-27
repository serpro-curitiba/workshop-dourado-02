package br.gov.serpro.sifap.calculo.application;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IpcaProviderService {

    private static final URI SGS_433 = URI.create("https://api.bcb.gov.br/dados/serie/bcdata.sgs.433/dados/ultimos/1?formato=json");
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final Pattern VALUE_PATTERN = Pattern.compile("\"valor\"\\s*:\\s*\"(?<value>[0-9]+,[0-9]+)\"");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private CachedIpca cached;
    private String lastAlert;

    public synchronized IpcaStatus latestStatus() {
        if (cached != null && cached.fetchedAt().plus(CACHE_TTL).isAfter(OffsetDateTime.now())) {
            return new IpcaStatus(cached.value(), cached.fetchedAt(), true, false, lastAlert);
        }
        try {
            BigDecimal value = fetchLatest();
            cached = new CachedIpca(value, OffsetDateTime.now());
            lastAlert = null;
            return new IpcaStatus(value, cached.fetchedAt(), false, false, null);
        } catch (IOException | InterruptedException | IllegalStateException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            lastAlert = "BCB_SGS_UNAVAILABLE: " + ex.getMessage();
            return new IpcaStatus(cached == null ? null : cached.value(),
                    cached == null ? null : cached.fetchedAt(), cached != null, true, lastAlert);
        }
    }

    private BigDecimal fetchLatest() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(SGS_433)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("BCB retornou HTTP " + response.statusCode());
        }
        Matcher matcher = VALUE_PATTERN.matcher(response.body());
        if (!matcher.find()) {
            throw new IllegalStateException("payload BCB sem campo valor");
        }
        return new BigDecimal(matcher.group("value").replace(',', '.'));
    }

    private record CachedIpca(BigDecimal value, OffsetDateTime fetchedAt) {
    }

    public record IpcaStatus(BigDecimal value, OffsetDateTime fetchedAt, boolean cacheHit,
                             boolean fallback, String alert) {
        public Optional<BigDecimal> optionalValue() {
            return Optional.ofNullable(value);
        }
    }
}