package com.youtube.core.testfixtures.support;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import java.util.List;

@TestComponent
public class TestSupport {

    private EntityManager entityManager;
    private EntityTransaction transaction;
    public JPAQueryFactory jpaQueryFactory;

    @Autowired
    private TestSupport(EntityManagerFactory entityManagerFactory, JPAQueryFactory jpaQueryFactory) {
        this.entityManager = entityManagerFactory.createEntityManager();
        this.transaction = entityManager.getTransaction();
        this.jpaQueryFactory = jpaQueryFactory;
    }

    public <T> T save(T entity) {
        transaction.begin();

        try {
            entityManager.persist(entity);
            entityManager.flush();
            transaction.commit();
            entityManager.clear();
        } catch (Exception e) {
            transaction.rollback();
        }

        return entity;
    }

    public <T> List<T> saveAll(T... entities) {
        transaction.begin();

        try {
            for (T entity : entities) {
                entityManager.persist(entity);
            }
            entityManager.flush();
            transaction.commit();
            entityManager.clear();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        }

        return List.of(entities);
    }
}
