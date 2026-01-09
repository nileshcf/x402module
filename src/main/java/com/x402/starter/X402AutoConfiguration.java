package com.x402.starter;

import com.x402.starter.config.X402Properties;
import com.x402.starter.interceptor.PaymentInterceptor;
import com.x402.starter.service.FacilitatorService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@EnableConfigurationProperties(X402Properties.class)
@ConditionalOnProperty(prefix = "x402", name = "client-id")
public class X402AutoConfiguration implements WebMvcConfigurer {

    @Bean
    public FacilitatorService facilitatorService(WebClient.Builder builder, X402Properties properties) {
        return new FacilitatorService(builder, properties);
    }

    @Bean
    public PaymentInterceptor paymentInterceptor(X402Properties properties, FacilitatorService facilitatorService) {
        return new PaymentInterceptor(properties, facilitatorService);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // The interceptor bean must be retrieved from context to be injected properly
    }

    @Bean
    public WebMvcConfigurer x402WebMvcConfigurer(PaymentInterceptor paymentInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(paymentInterceptor);
            }
        };
    }
}