package app.controllers;

import app.dtos.DTOMapper;
import app.entities.User;
import app.services.CalculationService;
import app.services.UserService;
import jakarta.persistence.EntityManagerFactory;
import io.javalin.http.Handler;

public class CalculationController {

    private final CalculationService calcService;
    private final UserService userService;

    // expose handlers as fields (assigned in ctor)
    public final Handler getAll;
    public final Handler add;
    public final Handler subtract;
    public final Handler multiply;
    public final Handler divide;
    public final Handler getMine;
    public final Handler deleteById;

    public CalculationController(EntityManagerFactory emf) {
        this.calcService = new CalculationService(emf);
        this.userService  = new UserService(emf);

        this.getAll = ctx -> {
            var list = calcService.getAll().stream()
                    .map(DTOMapper::toCalculationDTO)
                    .toList();
            ctx.json(list);
        };


        this.add = ctx -> {
            CalcReq body = ctx.bodyAsClass(CalcReq.class);
            String username = ctx.attribute("jwt.user");
            User user = userService.findByUsername(username);
            var saved = calcService.add(user, body.num1, body.num2);
            ctx.json(DTOMapper.toCalculationDTO(saved));
        };

        this.subtract = ctx -> {
            CalcReq body = ctx.bodyAsClass(CalcReq.class);
            String username = ctx.attribute("jwt.user");
            User user = userService.findByUsername(username);
            var saved = calcService.subtract(user, body.num1, body.num2);
            ctx.json(DTOMapper.toCalculationDTO(saved));
        };

        this.multiply = ctx -> {
            CalcReq body = ctx.bodyAsClass(CalcReq.class);
            String username = ctx.attribute("jwt.user");
            User user = userService.findByUsername(username);
            var saved = calcService.multiply(user, body.num1, body.num2);
            ctx.json(DTOMapper.toCalculationDTO(saved));
        };

        this.divide = ctx -> {
            CalcReq body = ctx.bodyAsClass(CalcReq.class);
            String username = ctx.attribute("jwt.user");
            User user = userService.findByUsername(username);
            var saved = calcService.divide(user, body.num1, body.num2);
            ctx.json(DTOMapper.toCalculationDTO(saved));
        };

        this.getMine = ctx -> {
            String username = ctx.attribute("jwt.user");
            User user = userService.findByUsername(username);
            var list = calcService.findAllByUser(user).stream()
                    .map(DTOMapper::toCalculationDTO)
                    .toList();
            ctx.json(list);
        };

        this.deleteById = ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            calcService.deleteById((int) id); // or long, match your @Id type
            ctx.json(java.util.Map.of("deletedId", id));
        };
    }



    // simple request payload for add/subtract/multiply/divide
    static class CalcReq { public double num1; public double num2; }
}