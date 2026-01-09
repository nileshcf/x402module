package com.x402.starter.interceptor;

import com.x402.starter.annotation.X402Gated;
import com.x402.starter.config.X402Properties;
import com.x402.starter.service.FacilitatorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.math.BigDecimal;

public class PaymentInterceptor implements HandlerInterceptor {

 private final X402Properties properties;
 private final FacilitatorService facilitatorService;

 public PaymentInterceptor(X402Properties properties, FacilitatorService facilitatorService) {
  this.properties = properties;
  this.facilitatorService = facilitatorService;
 }

 @Override
 public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
  if (!(handler instanceof HandlerMethod handlerMethod)) {
   return true;
  }

  X402Gated annotation = handlerMethod.getMethodAnnotation(X402Gated.class);
  if (annotation == null) {
   annotation = handlerMethod.getBeanType().getAnnotation(X402Gated.class);
  }

  if (annotation == null) {
   return true; // Not gated
  }

  // 1. Resolve Pricing Context (Annotation overrides > Global config)
  String finalPrice = annotation.price().isEmpty() ? properties.price().toString() : annotation.price();
  String finalNetwork = annotation.network().isEmpty() ? properties.network() : annotation.network();
  String finalAsset = annotation.assetAddress().isEmpty() ? properties.assetAddress() : annotation.assetAddress();

  // 2. Check for X-Payment Header
  String paymentToken = request.getHeader("X-Payment");

  if (paymentToken == null || paymentToken.isBlank()) {
   send402Challenge(response, finalPrice, finalNetwork, finalAsset);
   return false;
  }

  // 3. Verify and Settle
  boolean settled = facilitatorService.verifyAndSettle(paymentToken, finalPrice, finalNetwork);

  if (!settled) {
   send402Challenge(response, finalPrice, finalNetwork, finalAsset); // Or 403 Forbidden if token was invalid but present
   return false;
  }

  return true;
 }

 private void send402Challenge(HttpServletResponse response, String price, String network, String asset) {
  response.setStatus(402); // Payment Required
  response.setHeader("X402-Price", price);
  response.setHeader("X402-Network", network);
  response.setHeader("X402-Asset", asset);
  response.setHeader("X402-Wallet", properties.serverWalletAddress());
 }
}