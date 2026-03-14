package ru.yandex.practicum.category.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.category.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Page<Category> findAll(Pageable pageable);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByName(String name);
}