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
import java.util.Objects;
import java.util.stream.Collectors;

public class PublicController {

    private final CalculationService calcService;

    public final Handler info;
    public final Handler stats;
    public final Handler examples;

    public PublicController(EntityManagerFactory emf) {
        this.calcService = new CalculationService(emf);

        this.info = ctx -> {
            // everything non-null → Map.of is fine here
            ctx.json(Map.of(
                    "name", "CalcAPI",
                    "version", "1.0",
                    "time", Instant.now().toString()
            ));
        };

        this.stats = ctx -> {
            var all = calcService.getAll();

            var byOp = all.stream()
                    .collect(Collectors.groupingBy(Calculation::getOperation, Collectors.counting()));

            var latest = all.stream()
                    .filter(c -> c.getTimestamp() != null)
                    .max(Comparator.comparing(Calculation::getTimestamp))
                    .orElse(null);

            // Use a mutable map so null values won’t blow up (unlike Map.of)
            var payload = new LinkedHashMap<String, Object>();
            payload.put("total", all.size());
            payload.put("byOperation", byOp);
            payload.put("latest", latest != null ? DTOMapper.toCalculationDTO(latest) : null);

            ctx.json(payload);
        };

        this.examples = ctx -> {
            // include the context path so it works whether you mount at /api or something else
            String base = Objects.toString(ctx.contextPath(), "");
            ctx.json(Map.of(
                    "add", Map.of("method", "POST", "path", base + "/calc/add", "body", Map.of("num1", 2, "num2", 5)),
                    "subtract", Map.of("method", "POST", "path", base + "/calc/subtract", "body", Map.of("num1", 10, "num2", 3)),
                    "multiply", Map.of("method", "POST", "path", base + "/calc/multiply", "body", Map.of("num1", 6, "num2", 7)),
                    "divide", Map.of("method", "POST", "path", base + "/calc/divide", "body", Map.of("num1", 42, "num2", 6))
            ));
        };
    }
}