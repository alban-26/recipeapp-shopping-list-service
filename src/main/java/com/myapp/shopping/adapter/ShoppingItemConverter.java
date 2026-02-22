package com.myapp.shopping.adapter;

import com.myapp.shopping.adapter.database.entities.ShoppingItemEntity;
import com.myapp.shopping.domain.model.ShoppingItem;
import org.openapitools.model.ShoppingItemDto;

public interface ShoppingItemConverter extends Converter<ShoppingItem, ShoppingItemDto, ShoppingItemEntity> {

}
