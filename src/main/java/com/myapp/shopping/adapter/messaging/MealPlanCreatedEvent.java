package com.myapp.shopping.adapter.messaging;


import com.my.common.api.UserId;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.Collection;

@AllArgsConstructor
@Data
public class MealPlanCreatedEvent {

    Collection<RecipeIngredient> recipeIngredients;
    LocalDate startDate;
    LocalDate endDate;
    UserId userId;
}
