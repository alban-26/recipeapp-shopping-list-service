package com.myapp.shopping.domain.model;

import lombok.With;

@With
public record ShoppingItem(ShoppingItemId id, Product product, double quantity, Unit unit, boolean checked, int rank) {
}
