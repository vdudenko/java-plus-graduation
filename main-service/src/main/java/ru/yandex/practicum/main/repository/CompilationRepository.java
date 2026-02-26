package ru.yandex.practicum.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.main.entity.Compilation;

@Repository
public interface CompilationRepository extends JpaRepository<Compilation, Long> {



}