package com.myapp.shopping.domain.service;


import com.my.common.api.Repository;
import com.myapp.shopping.domain.model.ShoppingItem;
import com.myapp.shopping.domain.model.ShoppingItemId;
import com.myapp.shopping.domain.model.ShoppingList;
import com.myapp.shopping.domain.model.ShoppingListId;

public interface ShoppingListRepository extends Repository<ShoppingList, ShoppingListId> {

    ShoppingItem insertShoppingItem(ShoppingListId shoppingListId, ShoppingItem shoppingItem);

    ShoppingItem deleteShoppingItem(ShoppingListId shoppingListId, ShoppingItemId shoppingItemId);

    ShoppingItem updateShoppingItem(ShoppingListId shoppingListId, ShoppingItem shoppingItem);

    ShoppingItem rearrangeRank(ShoppingListId shoppingListId, ShoppingItem shoppingItem);

}
