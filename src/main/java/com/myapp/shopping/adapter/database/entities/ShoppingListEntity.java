package com.myapp.shopping.adapter.database.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingListEntity {
    private Long id;
    private String title;
    private LocalDate createdAt;
    private String userId;
    private String productOrderStrategy;

    private List<ShoppingItemEntity> items;

}