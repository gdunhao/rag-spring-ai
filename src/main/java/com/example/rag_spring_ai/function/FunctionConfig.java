package com.example.rag_spring_ai.function;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Configuration class that registers tool-provider beans for the LLM.
 *
 * In Spring AI 1.0.0 the recommended way to define tools is to annotate regular
 * methods with {@link Tool} and pass the containing object to
 * {@code ChatClient.prompt().tools(toolObject)}.
 *
 * Spring AI will:
 *  - Introspect every {@code @Tool}-annotated method on the object
 *  - Generate a JSON schema from the method signature
 *  - Deserialise the LLM's function-call arguments and invoke the method
 *  - Return the result to the LLM for final response generation
 */
@Configuration
public class FunctionConfig {

    // --- Request / Response records ---

    public record TicketRequest(String customerName, String issue, String priority) {}
    public record TicketResponse(String ticketId, String status, String message, String createdAt) {}

    public record OrderRequest(String orderId) {}
    public record OrderResponse(String orderId, String status, String estimatedDelivery, String items) {}

    public record WeatherRequest(String city) {}
    public record WeatherResponse(String city, double temperature, String condition, String unit) {}

    // ------------------------------------------------------------------
    // Tool classes — each public method annotated with @Tool is exposed
    // to the LLM as an invokable function.
    // ------------------------------------------------------------------

    /**
     * Support tools: ticket creation and order lookup.
     * Pass an instance of this class to {@code .tools(supportTools)}.
     */
    public static class SupportTools {

        @Tool(description = "Create a customer support ticket. Use this when the customer's issue cannot be resolved from the FAQ and needs human intervention.")
        public TicketResponse createTicket(TicketRequest request) {
            String ticketId = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            return new TicketResponse(
                    ticketId,
                    "OPEN",
                    "Ticket created for: " + request.issue() + " (Priority: " + request.priority() + ")",
                    LocalDateTime.now().toString()
            );
        }

        @Tool(description = "Look up the status of a customer order by order ID. Use this when a customer asks about their order status.")
        public OrderResponse lookupOrder(OrderRequest request) {
            Map<String, OrderResponse> orders = Map.of(
                    "ORD-001", new OrderResponse("ORD-001", "SHIPPED", "April 12, 2026", "CloudFlow Pro License x1"),
                    "ORD-002", new OrderResponse("ORD-002", "PROCESSING", "April 15, 2026", "CloudFlow Enterprise License x5"),
                    "ORD-003", new OrderResponse("ORD-003", "DELIVERED", "April 5, 2026", "CloudFlow Starter License x1")
            );
            return orders.getOrDefault(
                    request.orderId(),
                    new OrderResponse(request.orderId(), "NOT_FOUND", "N/A", "No order found with this ID")
            );
        }
    }

    /**
     * Weather tools: get current weather for a city.
     * Pass an instance of this class to {@code .tools(weatherTools)}.
     */
    public static class WeatherTools {

        @Tool(description = "Get the current weather for a given city. Use this when the user asks about weather conditions.")
        public WeatherResponse getWeather(WeatherRequest request) {
            Map<String, WeatherResponse> weather = Map.of(
                    "New York",      new WeatherResponse("New York",      18.5, "Partly Cloudy", "Celsius"),
                    "London",        new WeatherResponse("London",        12.0, "Rainy",          "Celsius"),
                    "Tokyo",         new WeatherResponse("Tokyo",         22.0, "Sunny",          "Celsius"),
                    "San Francisco", new WeatherResponse("San Francisco", 16.0, "Foggy",          "Celsius")
            );
            return weather.getOrDefault(
                    request.city(),
                    new WeatherResponse(request.city(), 20.0, "Clear", "Celsius")
            );
        }
    }

    @Bean
    public SupportTools supportTools() {
        return new SupportTools();
    }

    @Bean
    public WeatherTools weatherTools() {
        return new WeatherTools();
    }
}
