package com.myapp.shopping.adapter.messaging;

import com.myapp.shopping.domain.model.Unit;

public record RecipeIngredient(Ingredient ingredient, double quantity, Unit unit) {
}