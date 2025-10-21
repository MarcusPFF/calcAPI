package app.services;

import app.daos.CalculationDAO;
import app.entities.Calculation;
import app.entities.User;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class CalculationService {

    private final CalculationDAO calcDAO;

    public CalculationService(EntityManagerFactory emf) {
        this.calcDAO = new CalculationDAO(emf);
    }

    public Calculation add(User user, double num1, double num2) {
        return save(user, num1, num2, num1 + num2, "ADD");
    }

    public Calculation subtract(User user, double num1, double num2) {
        return save(user, num1, num2, num1 - num2, "SUBTRACT");
    }

    public Calculation multiply(User user, double num1, double num2) {
        return save(user, num1, num2, num1 * num2, "MULTIPLY");
    }

    public Calculation divide(User user, double num1, double num2) {
        if (num2 == 0) throw new ArithmeticException("Cannot divide by zero");
        return save(user, num1, num2, num1 / num2, "DIVIDE");
    }

    public Calculation findById(int id) {
        return calcDAO.findById(id);
    }

    public List<Calculation> findAllByUser(User user) {
        return calcDAO.findAllByUser(user);
    }

    private Calculation save(User user, double n1, double n2, double result, String operation) {
        Calculation c = new Calculation();
        c.setUser(user);
        c.setNum1(n1);
        c.setNum2(n2);
        c.setResult(result);
        c.setOperation(operation);
        c.setTimestamp(LocalDateTime.now());
        return calcDAO.create(c);
    }
}