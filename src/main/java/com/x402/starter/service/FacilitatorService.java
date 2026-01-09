package com.x402.starter.service;

import com.x402.starter.config.X402Properties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class FacilitatorService {

    private final WebClient webClient;
    private final X402Properties properties;

    public FacilitatorService(WebClient.Builder webClientBuilder, X402Properties properties) {
        this.properties = properties;
        this.webClient = webClientBuilder
                .defaultHeader("X-Client-ID", properties.clientId())
                .defaultHeader("X-Secret-Key", properties.secretKey())
                .build();
    }

    public boolean verifyAndSettle(String paymentToken, String requiredPrice, String network) {
        // Pseudo-payload structure
        var payload = new SettlementRequest(paymentToken, requiredPrice, network, properties.serverWalletAddress());

        return Boolean.TRUE.equals(this.webClient.post()
                .uri(properties.facilitator().settleUrl())
                .bodyValue(payload)
                .retrieve()
                .toEntity(Void.class)
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorReturn(false)
                .block()); // Blocking is acceptable in HandlerInterceptor for synchronous execution
    }

    record SettlementRequest(String token, String amount, String network, String receiver) {}
}