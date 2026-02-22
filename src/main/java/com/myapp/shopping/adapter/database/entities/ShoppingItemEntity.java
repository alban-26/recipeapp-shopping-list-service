package com.myapp.shopping.adapter.database.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShoppingItemEntity {
    private Long id;
    private Long shoppingListId;
    private String name;
    private String productCategory;
    private Double quantity;
    private String unit;
    private Boolean checked;
    private Integer rank;
}