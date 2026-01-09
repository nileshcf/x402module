x402-spring-boot-starter/
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.x402.starter
│   │   │       ├── X402AutoConfiguration.java       // Entry point
│   │   │       ├── annotation
│   │   │       │   └── X402Gated.java               // Marker Annotation
│   │   │       ├── config
│   │   │       │   └── X402Properties.java          // Configuration binding
│   │   │       ├── exception
│   │   │       │   └── PaymentRequiredException.java
│   │   │       ├── interceptor
│   │   │       │   └── PaymentInterceptor.java      // The Gatekeeper
│   │   │       ├── model
│   │   │       │   ├── PaymentContext.java          // DTO for merged config
│   │   │       │   └── FacilitatorResponse.java     // External API response
│   │   │       └── service
│   │           │   └── FacilitatorService.java      // WebClient Wrapper
│   │   └── resources
│   │       └── META-INF
│   │           └── spring
│   │               └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   └── test
│       ├── java
│       │   └── com.x402.starter
│       │       ├── interceptor
│       │       │   └── PaymentInterceptorTest.java  // Unit Tests
│       │       └── integration
│       │           └── X402IntegrationTest.java     // Integration Tests (WireMock)
│       └── resources
│           └── application-test.yml


# X402 Spring Boot Starter 🪙

**A drop-in monetization layer for Spring Boot APIs.**

This starter library allows developers to gatekeep API endpoints using the **HTTP 402 Payment Required** protocol. It acts as a middleware that intercepts requests, challenges the client for payment, and verifies settlement via an external Facilitator service before allowing access to your business logic.

---

## 🏗 Architecture & Flow

This library operates as a `HandlerInterceptor` within the Spring MVC request lifecycle. It decouples payment logic from your controllers.



1.  **Phase 1 (Challenge):** The client requests a resource. The library intercepts this, sees no payment header, and returns `402 Payment Required` with instructions (Price, Wallet, Network).
2.  **Phase 2 (Settlement):** The client pays on-chain and retries the request with a signature header (`X-Payment`).
3.  **Phase 3 (Verification):** The library validates the signature via the Facilitator service. If valid, the controller executes.

---

## 🚀 How to Utilize This Repository

This repository is a **library**, not a standalone application. You include it in other Spring Boot projects to add payment capabilities instantly.

### Step 1: Install the Library
Since this is a private starter, you must first build and install it into your local Maven repository.

```bash
# Clone this repository
git clone [https://github.com/your-org/x402-spring-boot-starter.git](https://github.com/your-org/x402-spring-boot-starter.git)

# Build and install to local .m2 folder
cd x402-spring-boot-starter
mvn clean install


Step 2: Add Dependency to Your Consumer App
Open your existing Spring Boot application (the one you want to monetize) and add this to pom.xml

<dependency>
    <groupId>com.x402</groupId>
    <artifactId>x402-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>

Step 3: Configure Your Application
Add the required configuration to your application.yml. This sets the default pricing and wallet destination for your API.

x402:
  # Your API Identity (for the Facilitator service)
  client-id: "my-service-id"
  secret-key: "super-secret-key-change-me"
  
  # Wallet & Asset Defaults
  server-wallet-address: "0x1234567890abcdef1234567890abcdef12345678"
  network: "ETH-MAINNET"
  asset-address: "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48" # USDC
  
  # Default Price per Request
  price: 1.00

  # Facilitator Service (The external system verifying the blockchain)
  facilitator:
    verify-url: "[https://api.x402-facilitator.com/verify](https://api.x402-facilitator.com/verify)"
    settle-url: "[https://api.x402-facilitator.com/settle](https://api.x402-facilitator.com/settle)"

Once configured, you can monetize any endpoint by adding a single annotation.

1. Basic Gating (Default Pricing)
Use the @X402Gated annotation to protect an endpoint. It will inherit the price and network from your application.yml


@RestController
@RequestMapping("/api")
public class StockController {

    // 🔒 Protected: Costs $1.00 (default)
    @GetMapping("/market-data")
    @X402Gated 
    public Map<String, Object> getMarketData() {
        return Map.of("AAPL", 150.00, "TSLA", 700.00);
    }
}

2. Custom Pricing (Per Endpoint)
You can override the global defaults for specific "premium" endpoints.

// 💎 Premium: Costs $50.00 on Polygon
    @GetMapping("/vip-report")
    @X402Gated(price = "50.00", network = "POLYGON", assetAddress = "0x...DAI")
    public Report getVipReport() {
        return generateExpensiveReport();
    }

3. Class-Level Gating
You can apply payment requirements to an entire controller.

@RestController
@RequestMapping("/admin")
@X402Gated(price = "10.00") // All methods in this class cost $10
public class AdminController {
    
    @GetMapping("/users")
    public List<User> getUsers() { ... }

    @GetMapping("/logs")
    public List<Log> getLogs() { ... }
}


