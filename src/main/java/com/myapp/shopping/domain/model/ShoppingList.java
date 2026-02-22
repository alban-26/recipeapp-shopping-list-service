package com.myapp.shopping.domain.model;

import com.my.common.api.UserId;
import lombok.With;

import java.time.LocalDate;
import java.util.List;

@With
public record ShoppingList(ShoppingListId id, String title, List<ShoppingItem> shoppingItems, LocalDate createdAt, UserId userId, ProductOrderStrategy productOrderStrategy) {
}
