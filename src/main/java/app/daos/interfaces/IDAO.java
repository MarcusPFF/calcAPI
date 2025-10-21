package app.daos.interfaces;

import java.util.List;

public interface IDAO<T, I> {
    //CREATE
    T create(T entity);

    //READ
    T findById(I id);
    List<T> getAll();

    //UPDATE
    T update(T entity);

    //DELETE
    void delete(I id);
}