package com.x402.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@ConfigurationProperties(prefix = "x402")
@Validated
public record X402Properties(
        @NotBlank String clientId,
        @NotBlank String secretKey,
        @NotBlank String serverWalletAddress,
        @NotBlank String network,
        @NotBlank String assetAddress,
        @NotNull BigDecimal price, // Default price
        Facilitator facilitator
) {
 public record Facilitator(
         @NotBlank String verifyUrl,
         @NotBlank String settleUrl
 ) {}
}