package ru.yandex.practicum.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.main.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Boolean existsByEmail(String email);
}