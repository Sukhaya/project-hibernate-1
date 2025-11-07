package com.game.repository;

import com.game.entity.Player;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

@Repository(value = "db")
public class PlayerRepositoryDB implements IPlayerRepository {
    private final SessionFactory sessionFactory;

    public PlayerRepositoryDB() {
        Properties properties = new Properties();
        properties.put(Environment.DRIVER, "com.p6spy.engine.spy.P6SpyDriver");
        properties.put(Environment.URL, "jdbc:p6spy:mysql://localhost:3306/rpg");
        properties.put(Environment.DIALECT, "org.hibernate.dialect.MySQLDialect");
        properties.put(Environment.USER, "root");
        properties.put(Environment.PASS, "134611sla");
        properties.put(Environment.HBM2DDL_AUTO, "update");

        sessionFactory = new Configuration()
                .setProperties(properties)
                .addAnnotatedClass(Player.class)
                .buildSessionFactory();
    }

    @Override
    public List<Player> getAll(int pageNumber, int pageSize) {
        return execute(session -> {
            NativeQuery<Player> query = session.createNativeQuery(
                    "SELECT * FROM rpg.player", Player.class);
            query.setFirstResult(pageNumber * pageSize);
            query.setMaxResults(pageSize);
            return query.list();
        });

    }

    @Override
    public int getAllCount() {
        return execute(session -> {
            Query<Long> query = session.createNamedQuery(
                    "player_getAllCount", Long.class);
            return Math.toIntExact(query.uniqueResult());
        });
    }

    @Override
    public Player save(Player player) {
        executeInTransaction(session -> session.save(player));
        return player;
    }

    @Override
    public Player update(Player player) {
        executeInTransaction(session -> session.update(player));
        return player;
    }

    @Override
    public Optional<Player> findById(long id) {
        return Optional.ofNullable(
                execute(session -> session.get(Player.class, id))
        );
    }

    @Override
    public void delete(Player player) {
        executeInTransaction(session -> session.delete(player));
    }

    @PreDestroy
    public void beforeStop() {
        sessionFactory.close();
    }


    /**
     * Универсальный метод для выполнения операций, не изменяющих данные (SELECT).
     * Открывает и закрывает сессию автоматически через try-with-resources.
     * Использует функциональный интерфейс Function, чтобы можно было передать логику извне.
     */
    private <T> T execute(Function<Session, T> function) {
        try (Session session = sessionFactory.openSession()) {
            return function.apply(session);
        }
    }


    /**
     * Универсальный метод для выполнения операций, изменяющих данные (INSERT, UPDATE, DELETE).
     * Открывает сессию, начинает транзакцию, коммитит и закрывает.
     * Использует Consumer, потому что ничего не возвращает.
     */
    private void executeInTransaction(Consumer<Session> consumer) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            consumer.accept(session);
            tx.commit();
        }
    }
}