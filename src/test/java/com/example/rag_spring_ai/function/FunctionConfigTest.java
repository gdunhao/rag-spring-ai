package com.example.rag_spring_ai.function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the FunctionConfig tool classes.
 * These are pure Java tests — no Spring context needed.
 */
class FunctionConfigTest {

    private FunctionConfig.SupportTools supportTools;
    private FunctionConfig.WeatherTools weatherTools;

    @BeforeEach
    void setUp() {
        supportTools = new FunctionConfig.SupportTools();
        weatherTools = new FunctionConfig.WeatherTools();
    }

    // ── SupportTools.createTicket ────────────────────────────────────────────

    @Test
    void createTicket_returnsOpenTicketWithTktPrefix() {
        var request = new FunctionConfig.TicketRequest("Alice", "Login issue", "HIGH");

        FunctionConfig.TicketResponse response = supportTools.createTicket(request);

        assertThat(response.status()).isEqualTo("OPEN");
        assertThat(response.ticketId()).startsWith("TKT-");
        assertThat(response.message()).contains("Login issue");
        assertThat(response.message()).contains("HIGH");
        assertThat(response.createdAt()).isNotBlank();
    }

    @Test
    void createTicket_ticketIdIsUniquePerCall() {
        var req = new FunctionConfig.TicketRequest("Bob", "Bug", "LOW");

        String id1 = supportTools.createTicket(req).ticketId();
        String id2 = supportTools.createTicket(req).ticketId();

        assertThat(id1).isNotEqualTo(id2);
    }

    // ── SupportTools.lookupOrder ─────────────────────────────────────────────

    @Test
    void lookupOrder_knownOrder_returnsCorrectStatus() {
        FunctionConfig.OrderResponse resp = supportTools.lookupOrder(
                new FunctionConfig.OrderRequest("ORD-001"));

        assertThat(resp.orderId()).isEqualTo("ORD-001");
        assertThat(resp.status()).isEqualTo("SHIPPED");
    }

    @Test
    void lookupOrder_knownOrder_ORD002_isProcessing() {
        FunctionConfig.OrderResponse resp = supportTools.lookupOrder(
                new FunctionConfig.OrderRequest("ORD-002"));

        assertThat(resp.status()).isEqualTo("PROCESSING");
    }

    @Test
    void lookupOrder_unknownOrder_returnsNotFound() {
        FunctionConfig.OrderResponse resp = supportTools.lookupOrder(
                new FunctionConfig.OrderRequest("ORD-999"));

        assertThat(resp.status()).isEqualTo("NOT_FOUND");
        assertThat(resp.orderId()).isEqualTo("ORD-999");
        assertThat(resp.items()).contains("No order found");
    }

    // ── WeatherTools.getWeather ──────────────────────────────────────────────

    @Test
    void getWeather_knownCity_returnsExpectedCondition() {
        FunctionConfig.WeatherResponse resp = weatherTools.getWeather(
                new FunctionConfig.WeatherRequest("London"));

        assertThat(resp.city()).isEqualTo("London");
        assertThat(resp.condition()).isEqualTo("Rainy");
        assertThat(resp.unit()).isEqualTo("Celsius");
    }

    @Test
    void getWeather_knownCity_Tokyo_isSunny() {
        FunctionConfig.WeatherResponse resp = weatherTools.getWeather(
                new FunctionConfig.WeatherRequest("Tokyo"));

        assertThat(resp.condition()).isEqualTo("Sunny");
        assertThat(resp.temperature()).isEqualTo(22.0);
    }

    @Test
    void getWeather_unknownCity_returnsDefaultClearCondition() {
        FunctionConfig.WeatherResponse resp = weatherTools.getWeather(
                new FunctionConfig.WeatherRequest("Unknown City"));

        assertThat(resp.city()).isEqualTo("Unknown City");
        assertThat(resp.condition()).isEqualTo("Clear");
        assertThat(resp.temperature()).isEqualTo(20.0);
    }
}

