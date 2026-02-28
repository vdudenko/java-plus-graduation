package ru.yandex.practicum.main.mapper;

import org.mapstruct.*;
import ru.yandex.practicum.main.dto.category.CategoryDto;
import ru.yandex.practicum.main.dto.category.NewCategoryDto;
import ru.yandex.practicum.main.dto.category.UpdateCategoryDto;
import ru.yandex.practicum.main.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryDto toCategoryDto(Category category);

    Category toEntity(CategoryDto categoryDto);

    Category toCategory(NewCategoryDto newCategoryDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCategory(UpdateCategoryDto updateCategoryDto, @MappingTarget Category category);
}
