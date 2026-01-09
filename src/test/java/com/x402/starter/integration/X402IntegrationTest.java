package com.x402.starter.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.x402.starter.annotation.X402Gated;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "x402.client-id=test-client",
        "x402.secret-key=test-secret",
        "x402.server-wallet-address=0xServerWallet",
        "x402.network=ETH-MAINNET",
        "x402.asset-address=0xUSDC",
        "x402.price=10.00"
        // URL properties are removed here and handled by @DynamicPropertySource below
})
@AutoConfigureMockMvc
class X402IntegrationTest {

    // 1. Programmatic WireMock Registration (Starts server first)
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    // 2. Dynamic Property Source (Injects the REAL port into Spring)
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("x402.facilitator.verify-url", () -> wireMock.baseUrl() + "/verify");
        registry.add("x402.facilitator.settle-url", () -> wireMock.baseUrl() + "/settle");
    }

    @Autowired MockMvc mockMvc;

    @Test
    void whenHeaderMissing_thenReturn402_andChallengeHeaders() throws Exception {
        mockMvc.perform(get("/protected"))
                .andExpect(status().is(402))
                .andExpect(header().string("X402-Price", "10.00"))
                .andExpect(header().string("X402-Network", "ETH-MAINNET"));
    }

    @Test
    void whenPaymentValid_thenReturn200() throws Exception {
        // Mock the Facilitator API (Success)
        wireMock.stubFor(post(urlEqualTo("/settle"))
                .willReturn(aResponse().withStatus(200)));

        mockMvc.perform(get("/protected")
                        .header("X-Payment", "valid_signature_payload"))
                .andExpect(status().isOk())
                .andExpect(content().string("Success"));
    }

    @Test
    void whenFacilitatorRejects_thenReturn402() throws Exception {
        // Mock the Facilitator API (Failure/Insufficient Funds)
        wireMock.stubFor(post(urlEqualTo("/settle"))
                .willReturn(aResponse().withStatus(400)));

        mockMvc.perform(get("/protected")
                        .header("X-Payment", "invalid_payload"))
                .andExpect(status().is(402));
    }

    // Dummy Application for Context
    @SpringBootApplication
    @RestController
    static class TestApp {
        @GetMapping("/protected")
        @X402Gated
        public String protectedEndpoint() {
            return "Success";
        }
    }
}