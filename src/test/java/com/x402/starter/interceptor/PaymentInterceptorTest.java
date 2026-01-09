package com.x402.starter.interceptor;

import com.x402.starter.annotation.X402Gated;
import com.x402.starter.config.X402Properties;
import com.x402.starter.service.FacilitatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.math.BigDecimal;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentInterceptorTest {

    @Mock
    private X402Properties properties;

    @Mock
    private FacilitatorService facilitatorService;

    private PaymentInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        // 1. Initialize the Interceptor with mocked dependencies
        interceptor = new PaymentInterceptor(properties, facilitatorService);

        // 2. Setup fresh request/response for each test
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        // 3. Setup default Global Properties behavior (Lenient because not all tests use all props)
        lenient().when(properties.serverWalletAddress()).thenReturn("0xGlobalWallet");
        lenient().when(properties.price()).thenReturn(new BigDecimal("10.00"));
        lenient().when(properties.network()).thenReturn("ETH-MAINNET");
        lenient().when(properties.assetAddress()).thenReturn("0xUSDC");
    }

    @Test
    void preHandle_ShouldPass_WhenHandlerIsNotMethod() throws Exception {
        // Example: Static resource handler (ResourceHttpRequestHandler)
        Object nonMethodHandler = new Object();

        boolean result = interceptor.preHandle(request, response, nonMethodHandler);

        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(200); // Status remains untouched
    }

    @Test
    void preHandle_ShouldPass_WhenMethodIsNotAnnotated() throws Exception {
        HandlerMethod handler = getHandlerMethod("publicEndpoint");

        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isTrue();
        verifyNoInteractions(facilitatorService); // No payment check needed
    }

    @Test
    void preHandle_ShouldReturn402_WhenHeaderIsMissing() throws Exception {
        HandlerMethod handler = getHandlerMethod("protectedEndpoint");

        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isFalse(); // Request halted
        assertThat(response.getStatus()).isEqualTo(402);

        // Assert Headers matched Global Config
        assertThat(response.getHeader("X402-Price")).isEqualTo("10.00");
        assertThat(response.getHeader("X402-Network")).isEqualTo("ETH-MAINNET");
        assertThat(response.getHeader("X402-Wallet")).isEqualTo("0xGlobalWallet");
    }

    @Test
    void preHandle_ShouldCallFacilitator_AndPass_WhenPaymentValid() throws Exception {
        HandlerMethod handler = getHandlerMethod("protectedEndpoint");
        request.addHeader("X-Payment", "valid_token_123");

        // Mock success from service
        when(facilitatorService.verifyAndSettle(eq("valid_token_123"), anyString(), anyString()))
                .thenReturn(true);

        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isTrue(); // Passed through
        assertThat(response.getStatus()).isNotEqualTo(402);
    }

    @Test
    void preHandle_ShouldReturn402_WhenFacilitatorReturnsFalse() throws Exception {
        HandlerMethod handler = getHandlerMethod("protectedEndpoint");
        request.addHeader("X-Payment", "insufficient_funds_token");

        // Mock failure from service
        when(facilitatorService.verifyAndSettle(anyString(), anyString(), anyString()))
                .thenReturn(false);

        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isFalse(); // Halted
        assertThat(response.getStatus()).isEqualTo(402);
    }

    @Test
    void preHandle_ShouldPrioritizeAnnotationOverrides() throws Exception {
        HandlerMethod handler = getHandlerMethod("expensiveEndpoint"); // Has overrides

        // Case A: Missing Header (Check if headers reflect overrides)
        boolean resultMissing = interceptor.preHandle(request, response, handler);
        assertThat(resultMissing).isFalse();
        assertThat(response.getHeader("X402-Price")).isEqualTo("50.00"); // From Annotation
        assertThat(response.getHeader("X402-Network")).isEqualTo("POLYGON"); // From Annotation

        // Reset response for next check
        response = new MockHttpServletResponse();
        request.addHeader("X-Payment", "poly_token");

        // Case B: Present Header (Check if service calls use overrides)
        when(facilitatorService.verifyAndSettle(eq("poly_token"), eq("50.00"), eq("POLYGON")))
                .thenReturn(true);

        boolean resultPresent = interceptor.preHandle(request, response, handler);
        assertThat(resultPresent).isTrue();
    }

    // --- Helper to reflectively get HandlerMethod ---
    private HandlerMethod getHandlerMethod(String methodName) throws NoSuchMethodException {
        Method method = TestController.class.getMethod(methodName);
        return new HandlerMethod(new TestController(), method);
    }

    // --- Dummy Controller for Testing ---
    static class TestController {

        public void publicEndpoint() {}

        @X402Gated
        public void protectedEndpoint() {}

        @X402Gated(price = "50.00", network = "POLYGON", assetAddress = "0xPolyUSDC")
        public void expensiveEndpoint() {}
    }
}