package app.controllers;

import app.dtos.DTOMapper;
import app.entities.User;
import app.services.CalculationService;
import app.services.UserService;
import io.javalin.http.Handler;
import jakarta.persistence.EntityManagerFactory;

import java.util.Map;

public class CalculationController {
    private final CalculationService calcService;
    private final UserService userService;

    public CalculationController(EntityManagerFactory emf) {
        this.calcService = new CalculationService(emf);
        this.userService = new UserService(emf);
    }

    private static class CalcReq {
        public double num1;
        public double num2;
    }

    public Handler add() {
        return ctx -> {
            CalcReq body = ctx.bodyAsClass(CalcReq.class);
            String username = ctx.attribute("jwt.user");
            User user = userService.findByUsername(username);
            var saved = calcService.add(user, body.num1, body.num2);
            ctx.json(DTOMapper.toCalculationDTO(saved));
        };
    }

    public Handler subtract() {
        return ctx -> {
            CalcReq body = ctx.bodyAsClass(CalcReq.class);
            String username = ctx.attribute("jwt.user");
            User user = userService.findByUsername(username);
            var saved = calcService.subtract(user, body.num1, body.num2);
            ctx.json(DTOMapper.toCalculationDTO(saved));
        };
    }

    public Handler multiply() {
        return ctx -> {
            CalcReq body = ctx.bodyAsClass(CalcReq.class);
            String username = ctx.attribute("jwt.user");
            User user = userService.findByUsername(username);
            var saved = calcService.multiply(user, body.num1, body.num2);
            ctx.json(DTOMapper.toCalculationDTO(saved));
        };
    }

    public Handler divide() {
        return ctx -> {
            CalcReq body = ctx.bodyAsClass(CalcReq.class);
            String username = ctx.attribute("jwt.user");
            User user = userService.findByUsername(username);
            var saved = calcService.divide(user, body.num1, body.num2);
            ctx.json(DTOMapper.toCalculationDTO(saved));
        };
    }

    public Handler getAll() {
        return ctx -> {
            var all = calcService.getAll().stream().map(DTOMapper::toCalculationDTO).toList();
            ctx.json(all);
        };
    }

    public Handler getMine() {
        return ctx -> {
            String username = ctx.attribute("jwt.user");
            User user = userService.findByUsername(username);
            var mine = calcService.findAllByUser(user).stream().map(DTOMapper::toCalculationDTO).toList();
            ctx.json(mine);
        };
    }

    public Handler deleteById() {
        return ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            calcService.deleteById(id);
            ctx.json(Map.<String, Object>of("deletedId", id));
        };
    }
}