package ru.yandex.practicum.category.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.category.service.CategoryService;
import ru.yandex.practicum.interaction.dto.category.CategoryDto;
import ru.yandex.practicum.interaction.dto.category.NewCategoryDto;
import ru.yandex.practicum.interaction.dto.category.UpdateCategoryDto;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminCategoryController {
    private final CategoryService categoryService;

    @PostMapping("/categories")
    public ResponseEntity<CategoryDto> create(@Valid @RequestBody NewCategoryDto newCategoryDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.addCategory(newCategoryDto));
    }

    @DeleteMapping("/categories/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long catId) {
        categoryService.delete(catId);
    }

    @PatchMapping("/categories/{catId}")
    public CategoryDto update(@PathVariable Long catId,
                              @Valid @RequestBody UpdateCategoryDto updateCategoryDto) {
        return categoryService.updateCategory(catId, updateCategoryDto);
    }

    @GetMapping("/categories/{categoryId}/exists")
    public boolean exists(@PathVariable Long categoryId) {
        return categoryService.exists(categoryId);
    }
}