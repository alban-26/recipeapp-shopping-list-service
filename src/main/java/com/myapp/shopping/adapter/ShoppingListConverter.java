package com.myapp.shopping.adapter;


import com.my.common.api.EventConverter;
import com.myapp.shopping.adapter.database.entities.ShoppingListEntity;
import com.myapp.shopping.adapter.messaging.MealPlanCreatedEvent;
import com.myapp.shopping.domain.model.ShoppingList;
import org.openapitools.model.ShoppingListDto;


public interface ShoppingListConverter extends Converter<ShoppingList, ShoppingListDto, ShoppingListEntity>, EventConverter<ShoppingList, MealPlanCreatedEvent> {
}
