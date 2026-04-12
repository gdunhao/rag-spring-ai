package com.example.rag_spring_ai.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * HTTP boundary logger.
 *
 * Emits structured log lines that make the Frontend → Controller leg of the
 * integration flow visible without polluting individual controllers with
 * boilerplate logging code.
 *
 * Log markers used across the application:
 *   [HTTP→]      incoming HTTP request (frontend → backend)
 *   [HTTP←]      outgoing HTTP response (backend → frontend)
 *   [→VectorDB]  writing to or searching the vector store
 *   [←VectorDB]  result returned from the vector store
 *   [→Ollama]    LLM / embedding call sent to Ollama
 *   [←Ollama]    response received from Ollama
 *   [⚙Tool]      Spring AI @Tool function invoked by the LLM
 *   [Memory]     chat-memory session operations
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    /** Attribute key used to store the request start time on the servlet request. */
    private static final String START_ATTR = "rag.requestStart";

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {

        request.setAttribute(START_ATTR, System.currentTimeMillis());

        String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        log.info("[HTTP→] {} {}{}", request.getMethod(), request.getRequestURI(), query);

        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex) {

        long start = (Long) request.getAttribute(START_ATTR);
        long elapsed = System.currentTimeMillis() - start;

        if (ex != null) {
            log.warn("[HTTP←] {} {} | status={} | elapsed={}ms | error='{}'",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), elapsed, ex.getMessage());
        } else {
            log.info("[HTTP←] {} {} | status={} | elapsed={}ms",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), elapsed);
        }
    }
}

