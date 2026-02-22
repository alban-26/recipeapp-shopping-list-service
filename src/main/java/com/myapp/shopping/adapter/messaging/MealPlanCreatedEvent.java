package com.myapp.shopping.adapter.messaging;


import com.my.common.api.UserId;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collection;

@AllArgsConstructor
@Data
public class MealPlanCreatedEvent {

    Collection<RecipeIngredient> recipeIngredients;
    UserId userId;
}
