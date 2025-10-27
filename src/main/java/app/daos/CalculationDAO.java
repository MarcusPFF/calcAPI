package app.daos;

import app.daos.interfaces.IDAO;
import app.entities.Calculation;
import app.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class CalculationDAO implements IDAO<Calculation, Integer> {

    private final EntityManagerFactory emf;

    public CalculationDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public Calculation create(Calculation entity) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            return entity;
        }
    }

    @Override
    public Calculation findById(Integer id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Calculation.class, id);
        }
    }

    @Override
    public List<Calculation> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                    "SELECT DISTINCT c " +
                            "FROM Calculation c " +
                            "LEFT JOIN FETCH c.user " +
                            "ORDER BY c.timestamp DESC",
                    Calculation.class
            ).getResultList();
        }
    }

    @Override
    public Calculation update(Calculation entity) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Calculation updated = em.merge(entity);
            em.getTransaction().commit();
            return updated;
        }
    }

    @Override
    public void delete(Integer id) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Calculation c = em.find(Calculation.class, id);
            if (c != null) em.remove(c);
            em.getTransaction().commit();
        }
    }

    public List<Calculation> findAllByUser(User user) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT c FROM Calculation c WHERE c.user = :user", Calculation.class)
                    .setParameter("user", user)
                    .getResultList();
        }
    }
}
