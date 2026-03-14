package ru.yandex.practicum.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.user.entity.User;

import java.util.Collection;
import java.util.Map;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Boolean existsByEmail(String email);

    @Query("SELECT u.id, u.name FROM User u WHERE u.id IN :userIds")
    Map<Long, String> findNamesByIds(@Param("userIds") Collection<Long> userIds);
}