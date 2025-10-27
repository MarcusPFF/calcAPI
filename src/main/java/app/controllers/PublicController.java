package app.controllers;

import app.dtos.DTOMapper;
import app.entities.Calculation;
import app.services.CalculationService;
import io.javalin.http.Handler;
import jakarta.persistence.EntityManagerFactory;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PublicController {
    private final CalculationService calcService;

    public PublicController(EntityManagerFactory emf) {
        this.calcService = new CalculationService(emf);
    }

    public Handler info() {
        return ctx -> ctx.json(Map.of(
                "name", "CalcAPI",
                "version", "1.0",
                "time", Instant.now().toString()
        ));
    }

    public Handler stats() {
        return ctx -> {
            var all = calcService.getAll();
            var byOp = all.stream()
                    .collect(Collectors.groupingBy(Calculation::getOperation, Collectors.counting()));
            var latest = all.stream()
                    .max(Comparator.comparing(Calculation::getTimestamp))
                    .orElse(null);

            var ordered = new LinkedHashMap<String, Object>();
            ordered.put("total", all.size());
            ordered.put("byOperation", byOp);
            ordered.put("latest", latest != null ? DTOMapper.toCalculationDTO(latest) : null);

            ctx.json(ordered);
        };
    }

    public Handler examples() {
        return ctx -> ctx.json(Map.of(
                "add", Map.of("method", "POST", "path", "/api/calc/add", "body", Map.of("num1", 2, "num2", 5)),
                "subtract", Map.of("method", "POST", "path", "/api/calc/subtract", "body", Map.of("num1", 10, "num2", 3)),
                "multiply", Map.of("method", "POST", "path", "/api/calc/multiply", "body", Map.of("num1", 6, "num2", 7)),
                "divide", Map.of("method", "POST", "path", "/api/calc/divide", "body", Map.of("num1", 42, "num2", 6))
        ));
    }
}