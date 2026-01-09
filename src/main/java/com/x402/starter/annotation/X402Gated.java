package com.x402.starter.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface X402Gated {
 String price() default "";       // Overrides x402.price
 String assetAddress() default ""; // Overrides x402.asset-address
 String network() default "";      // Overrides x402.network
}