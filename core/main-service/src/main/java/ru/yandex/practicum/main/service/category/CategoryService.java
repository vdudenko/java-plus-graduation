package ru.yandex.practicum.main.service.category;

import ru.yandex.practicum.main.dto.category.CategoryDto;
import ru.yandex.practicum.main.dto.category.NewCategoryDto;
import ru.yandex.practicum.main.dto.category.UpdateCategoryDto;

import java.util.List;

public interface CategoryService {

    void delete(Long id);

    List<CategoryDto> getAll(int from, int size);

    CategoryDto getById(Long id);

    CategoryDto addCategory(NewCategoryDto newCategoryDto);

    CategoryDto updateCategory(Long id, UpdateCategoryDto updateCategoryDto);
}
