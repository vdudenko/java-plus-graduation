package ru.yandex.practicum.category.service;

import ru.yandex.practicum.interaction.dto.category.CategoryDto;
import ru.yandex.practicum.interaction.dto.category.NewCategoryDto;
import ru.yandex.practicum.interaction.dto.category.UpdateCategoryDto;

import java.util.List;

public interface CategoryService {

    void delete(Long id);

    List<CategoryDto> getAll(int from, int size);

    CategoryDto getById(Long id);

    CategoryDto addCategory(NewCategoryDto newCategoryDto);

    CategoryDto updateCategory(Long id, UpdateCategoryDto updateCategoryDto);

    boolean exists(Long categoryId);
}
