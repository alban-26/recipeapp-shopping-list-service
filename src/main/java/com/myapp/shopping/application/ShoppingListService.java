package com.myapp.shopping.application;

import com.my.common.api.AbstractAccess;
import com.myapp.shopping.domain.model.ShoppingItem;
import com.myapp.shopping.domain.model.ShoppingItemId;
import com.myapp.shopping.domain.model.ShoppingList;
import com.myapp.shopping.domain.model.ShoppingListId;
import com.myapp.shopping.domain.service.ShoppingListRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static java.lang.String.format;

@ApplicationScoped
@Slf4j
public class ShoppingListService extends AbstractAccess<ShoppingListRepository, ShoppingList, ShoppingListId> {

    protected ShoppingListService() {
        super(null);
    }


    @Inject
    public ShoppingListService(ShoppingListRepository recipeRepository) {
        super(recipeRepository);
    }

    @Override
    protected boolean isNew(ShoppingList item) {
        return item.id().id() == 0;
    }

    @Override
    protected ShoppingListId getId(ShoppingList item) {
        return item.id();
    }


    public ShoppingItem addShoppingItem(ShoppingListId shoppingListId, ShoppingItem shoppingItem) {
        return repository.insertShoppingItem(shoppingListId, shoppingItem);
    }

    public void removeShoppingItem(ShoppingListId shoppingListId, ShoppingItemId shoppingItemId) {
        ShoppingItem removed = repository.deleteShoppingItem(shoppingListId, shoppingItemId);

        log.info("Removed item %s from list {} {}", shoppingItemId.id(), shoppingListId.id());

    }

    public ShoppingItem updateShoppingItem(ShoppingListId shoppingListId, ShoppingItem shoppingItem) {
        return repository.updateShoppingItem(shoppingListId, shoppingItem);
    }

    public ShoppingList rearrangeItems(
            ShoppingList list,
            int fromIndex,
            int toIndex
    ) {
        List<ShoppingItem> items = list.shoppingItems();

        if (fromIndex < 0 || fromIndex >= items.size()
                || toIndex < 0 || toIndex >= items.size()) {
            throw new IndexOutOfBoundsException(
                    format(
                            "Rearranging for shopping list %d failed since invalid index was given",
                            list.id().id()
                    )
            );
        }

        List<ShoppingItem> reordered = new ArrayList<>(items);

        ShoppingItem movedItem = reordered.remove(fromIndex);
        reordered.add(toIndex, movedItem);

        List<ShoppingItem> newList = IntStream.range(0, reordered.size())
                .mapToObj(i -> reordered.get(i).withRank(i))
                .toList();




        newList.forEach(item -> repository.rearrangeRank(list.id(), item));

        return list.withShoppingItems(newList);
    }


    public ShoppingList reorderByCommon(ShoppingList list) {
        List<ShoppingItem> sorted = list.shoppingItems().stream()
                .sorted(Comparator.comparingInt(item -> item.product().category().getRank()))
                .toList();

        List<ShoppingItem> reranked = IntStream.range(0, sorted.size())
                .mapToObj(i -> sorted.get(i).withRank(i))
                .toList();

        reranked.forEach(item -> repository.rearrangeRank(list.id(), item));

        return list.withShoppingItems(reranked);
    }



}
