package ru.yandex.practicum.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.event.entity.Compilation;

@Repository
public interface CompilationRepository extends JpaRepository<Compilation, Long> {}