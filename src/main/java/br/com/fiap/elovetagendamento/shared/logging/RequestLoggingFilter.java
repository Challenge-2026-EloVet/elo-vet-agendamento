package br.com.fiap.elovetagendamento.shared.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Registra cada requisição HTTP recebida: método, rota, status da resposta e tempo de execução.
 * Respostas 4xx são registradas como WARN e 5xx como ERROR.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long inicio = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long duracao = System.currentTimeMillis() - inicio;
            String rota = request.getQueryString() == null
                    ? request.getRequestURI()
                    : request.getRequestURI() + "?" + request.getQueryString();
            int status = response.getStatus();

            if (status >= 500) {
                log.error("HTTP {} {} -> {} ({} ms)", request.getMethod(), rota, status, duracao);
            } else if (status >= 400) {
                log.warn("HTTP {} {} -> {} ({} ms)", request.getMethod(), rota, status, duracao);
            } else {
                log.info("HTTP {} {} -> {} ({} ms)", request.getMethod(), rota, status, duracao);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Pré-requisições de CORS do navegador não agregam informação ao log
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}
