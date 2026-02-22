package com.myapp.shopping.adapter.database;

import com.my.common.api.UserId;
import com.myapp.shopping.adapter.ShoppingItemConverter;
import com.myapp.shopping.adapter.ShoppingListConverter;
import com.myapp.shopping.adapter.database.entities.ShoppingItemEntity;
import com.myapp.shopping.adapter.database.entities.ShoppingListEntity;
import com.myapp.shopping.domain.model.ShoppingItem;
import com.myapp.shopping.domain.model.ShoppingItemId;
import com.myapp.shopping.domain.model.ShoppingList;
import com.myapp.shopping.domain.model.ShoppingListId;
import com.myapp.shopping.domain.service.ShoppingListRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class ShoppingListRepositoryImpl implements ShoppingListRepository {

    private final ShoppingListMapper shoppingListMapper;
    private final ShoppingListConverter shoppingListConverter;
    private final ShoppingItemConverter shoppingItemConverter;

    @Inject
    public ShoppingListRepositoryImpl(ShoppingListMapper shoppingListMapper,
                                      ShoppingListConverter shoppingListConverter,
                                      ShoppingItemConverter shoppingItemConverter) {
        this.shoppingListMapper = shoppingListMapper;
        this.shoppingListConverter = shoppingListConverter;
        this.shoppingItemConverter = shoppingItemConverter;
    }

    @Override
    public Optional<ShoppingList> findById(ShoppingListId shoppingListId) {
        ShoppingListEntity entity = shoppingListMapper.findShoppingListById(shoppingListId.id());
        if (entity == null) return Optional.empty();
        return Optional.of(shoppingListConverter.entityToDomain(entity));
    }

    @Override
    public List<ShoppingList> findAll() {
        List<ShoppingListEntity> entities = shoppingListMapper.findAllShoppingLists();
        return entities.stream()
                .map(entity -> {
                    List<ShoppingItemEntity> items = shoppingListMapper.findEntriesByShoppingListId(entity.getId());
                    entity.setItems(items);
                    return shoppingListConverter.entityToDomain(entity);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ShoppingList> findAllByUser(UserId userId) {
        List<ShoppingListEntity> entities = shoppingListMapper.findAllShoppingListsByUser(userId.value());
        return entities.stream()
                .map(entity -> {
                    List<ShoppingItemEntity> items = shoppingListMapper.findEntriesByShoppingListId(entity.getId());
                    entity.setItems(items);
                    return shoppingListConverter.entityToDomain(entity);
                })
                .collect(Collectors.toList());
    }

    @Override
    public ShoppingList save(ShoppingList shoppingList) {
        ShoppingListEntity shoppingListEntity = shoppingListConverter.domainToEntity(shoppingList);
        shoppingListMapper.insertShoppingList(shoppingListEntity);

        // Link items via the join table
        for (ShoppingItemEntity item : shoppingListEntity.getItems()) {
            if (item.getId() == null || item.getId() == 0) {
                // Insert item if it doesn’t exist
                shoppingListMapper.insertShoppingItem(item);
            }
            // Create the join (shopping_list_item) record
            shoppingListMapper.linkItemToList(
                    shoppingListEntity.getId(),
                    item.getId(),
                    item.getQuantity(),
                    item.getUnit(),
                    item.getChecked(),
                    item.getRank()
            );
        }

        return shoppingListConverter.entityToDomain(shoppingListEntity);
    }

    @Override
    public ShoppingList update(ShoppingList shoppingList) {
        ShoppingListEntity entity = shoppingListConverter.domainToEntity(shoppingList);
        shoppingListMapper.updateShoppingList(entity);

        // Update or insert items in join table
        for (ShoppingItemEntity item : entity.getItems()) {
            if (item.getId() == null) {
                shoppingListMapper.insertShoppingItem(item);
                shoppingListMapper.linkItemToList(
                        entity.getId(),
                        item.getId(),
                        item.getQuantity(),
                        item.getUnit(),
                        item.getChecked(),
                        item.getRank()
                );
            } else {
                shoppingListMapper.updateListEntry(
                        entity.getId(),
                        item.getId(),
                        item.getQuantity(),
                        item.getUnit(),
                        item.getChecked(),
                        item.getRank()
                );
            }
        }

        return shoppingListConverter.entityToDomain(entity);
    }

    @Override
    public void deleteById(ShoppingListId shoppingListId) {
        // Delete join table entries first
        List<ShoppingItemEntity> items = shoppingListMapper.findEntriesByShoppingListId(shoppingListId.id());
        for (ShoppingItemEntity item : items) {
            shoppingListMapper.deleteEntryFromList(shoppingListId.id(), item.getId());
        }
        shoppingListMapper.deleteShoppingList(shoppingListId.id());
    }

    @Override
    public ShoppingItem insertShoppingItem(ShoppingListId shoppingListId, ShoppingItem shoppingItem) {
        ShoppingItemEntity entity = shoppingItemConverter.domainToEntity(shoppingItem);
        entity.setShoppingListId(shoppingListId.id());
        if (entity.getId() == null || entity.getId() == 0) {
            shoppingListMapper.insertShoppingItem(entity);
        }

        shoppingListMapper.linkItemToList(
                shoppingListId.id(),
                entity.getId(),
                entity.getQuantity(),
                entity.getUnit(),
                entity.getChecked(),
                entity.getRank()
        );

        return shoppingItemConverter.entityToDomain(entity);
    }


    @Override
    public ShoppingItem deleteShoppingItem(ShoppingListId shoppingListId, ShoppingItemId shoppingItemId) {
        // Try to find the item in the list
        ShoppingItemEntity entity = shoppingListMapper.findEntriesByShoppingListId(shoppingListId.id())
                .stream()
                .filter(item -> item.getId().equals(shoppingItemId.id()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        String.format("Item %d not found in list %d", shoppingItemId.id(), shoppingListId.id())
                ));

        // Delete the link
        shoppingListMapper.deleteEntryFromList(shoppingListId.id(), shoppingItemId.id());

        return shoppingItemConverter.entityToDomain(entity);
    }

    @Override
    public ShoppingItem updateShoppingItem(ShoppingListId shoppingListId, ShoppingItem shoppingItem) {
        shoppingListMapper.updateListEntry(shoppingListId.id(), shoppingItem.id().id(), shoppingItem.quantity(), shoppingItem.unit().name(), shoppingItem.checked(), shoppingItem.rank());
        return shoppingItem;
    }

    @Override
    public ShoppingItem rearrangeRank(ShoppingListId shoppingListId, ShoppingItem shoppingItem) {
        log.info("Rearrange rank of shopping item {} of shoppinglist id {} to rank {}", shoppingItem.product().name(), shoppingListId, shoppingItem.rank());
        shoppingListMapper.updateRank(shoppingListId.id(), shoppingItem.id().id(), shoppingItem.rank());
        return shoppingItem;
    }


}
