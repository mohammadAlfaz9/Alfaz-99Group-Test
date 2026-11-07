package com.example.userservice;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


@Repository
public class UserRepository {

    private final List<User> users = new ArrayList<>();
    private int nextId = 1;

    public synchronized User save(String name) {
        long now = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
        User user = new User(nextId++, name, now, now);
        users.add(user);
        return user;
    }

    public synchronized Optional<User> findById(int id) {
        return users.stream().filter(u -> u.id() == id).findFirst();
    }
    public synchronized List<User> findAll(int pageNum, int pageSize) {
        return users.stream()
                .sorted(Comparator.comparingLong(User::createdAt).thenComparingInt(User::id).reversed())
                .skip((long) (pageNum - 1) * pageSize)
                .limit(pageSize)
                .toList();
    }
}
