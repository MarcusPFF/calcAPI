package app.services;

import app.daos.CalculationDAO;
import app.entities.Calculation;
import app.entities.User;
import app.exceptions.ApiException;
import app.exceptions.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class CalculationService {

    private final CalculationDAO calcDAO;
    private final EntityManagerFactory emf; // <-- keep a reference

    public CalculationService(EntityManagerFactory emf) {
        this.emf = emf;                      // <-- store it
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

    public Calculation divide(User user, double num1, double num2) throws ValidationException {
        if (num2 == 0) {
            throw new ValidationException("Cannot divide by zero");
        }
        return save(user, num1, num2, num1 / num2, "DIVIDE");
    }

    public Calculation findById(int id) {
        return calcDAO.findById(id);
    }

    public List<Calculation> findAllByUser(User user) {
        return calcDAO.findAllByUser(user);
    }


    public List<Calculation> getAll() {
        return calcDAO.getAll();
    }

    public void deleteById(int id) throws ApiException {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Calculation c = em.find(Calculation.class, id);
            if (c == null) {
                em.getTransaction().rollback();
                throw new ApiException(404, "Calculation not found");
            }

            em.remove(c);
            em.getTransaction().commit();
        } catch (RuntimeException | ApiException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
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