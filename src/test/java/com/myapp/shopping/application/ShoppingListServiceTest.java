package com.myapp.shopping.application;

import com.my.common.api.UserId;
import com.myapp.shopping.domain.model.*;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTestResource(EmbeddedPostgresResource.class)
public class ShoppingListServiceTest {

    private static final UserId USER_ID = new UserId("1");


    @Inject
    ShoppingListService shoppingListService;

    @Inject
    Flyway flyway;

    @BeforeEach
    public void setupDatabase() {
        flyway.clean();
        flyway.migrate();
    }

    @AfterAll
    public void cleanupAfterTests() {
        flyway.clean();
    }

    @Test
    public void testGetShoppingListById() {
        Optional<ShoppingList> shoppingList = shoppingListService.getById(new ShoppingListId(1L));

        assertTrue(shoppingList.isPresent());
        assertEquals("Test Shopping List", shoppingList.get().title());
    }

    @Test
    public void testGetAllShoppingLists() {
        var shoppingLists = shoppingListService.getAll();

        assertEquals(2, shoppingLists.size());

        ShoppingList shoppingList = shoppingLists.getFirst();
        assertEquals("Test Shopping List", shoppingList.title());
    }


    @Test
    public void testAddShoppingItemToExistingList() {
        ShoppingListId listId = new ShoppingListId(1L);
        Optional<ShoppingList> optional = shoppingListService.getById(listId);
        assertTrue(optional.isPresent(), "Shopping list should exist before inserting item");

        ShoppingList existing = optional.get();
        int initialSize = existing.shoppingItems().size();

        // Use an existing shopping item ID (e.g., Salt with ID 7)
        ShoppingItem newItem = new ShoppingItem(
                new ShoppingItemId(7L), // Use an existing ID
                new Product("Salt", ProductCategory.SPICES),
                1,
                Unit.BUNCH,
                false,
                1
        );

        ShoppingItem savedItem = shoppingListService.addShoppingItem(listId, newItem);

        assertNotNull(savedItem.id());

        Optional<ShoppingList> updatedOpt = shoppingListService.getById(listId);
        assertTrue(updatedOpt.isPresent());

        ShoppingList updatedList = updatedOpt.get();
        assertEquals(initialSize + 1, updatedList.shoppingItems().size());
        assertTrue(
                updatedList.shoppingItems().stream()
                        .anyMatch(item -> item.product().name().equals("Salt") && item.quantity() == 1)
        );
    }

    @Test
    public void testCreateShoppingList() {
        ShoppingItem item1 = new ShoppingItem(new ShoppingItemId(3L), new Product("Milk", ProductCategory.DAIRY), 2, Unit.JAR, false,1);
        ShoppingItem item2 = new ShoppingItem(new ShoppingItemId(8L), new Product("Pepper", ProductCategory.SPICES), 1, Unit.JAR, false, 2);
        ShoppingList newShoppingList = new ShoppingList(new ShoppingListId(0), "New Shopping List", List.of(item1, item2), LocalDate.now(), USER_ID, ProductOrderStrategy.STANDARD);

        ShoppingList saved = shoppingListService.save(newShoppingList);

        assertNotNull(saved.id());
        assertEquals("New Shopping List", saved.title());
        assertEquals(2, saved.shoppingItems().size());

        Optional<ShoppingList> fetched = shoppingListService.getById(saved.id());
        assertTrue(fetched.isPresent());
        assertEquals("New Shopping List", fetched.get().title());
    }

    @Test
    public void testRearrangeItems() {

        Optional<ShoppingList> shoppingList = shoppingListService.getById(new ShoppingListId(1));

        assertTrue(shoppingList.isPresent());
        List<ShoppingItem> shoppingItems = shoppingList.get().shoppingItems();

        assertEquals(1L, shoppingItems.get(0).id().id());
        assertEquals(3L, shoppingItems.get(1).id().id());
        assertEquals(5L, shoppingItems.get(2).id().id());

        ShoppingList reorderedList = shoppingListService.rearrangeItems(shoppingList.get(), 2, 0);

        assertEquals(5L, reorderedList.shoppingItems().get(0).id().id());
        assertEquals(1L, reorderedList.shoppingItems().get(1).id().id());
        assertEquals(3L, reorderedList.shoppingItems().get(2).id().id());

    }

    @Test
    public void testRemoveShoppingListById() {
        ShoppingListId toDeleteId = new ShoppingListId(2L);
        Optional<ShoppingList> toDeleteOpt = shoppingListService.getById(toDeleteId);
        assertTrue(toDeleteOpt.isPresent());

        ShoppingList toDelete = toDeleteOpt.get();

        shoppingListService.delete(toDelete);  // Assuming delete(T item) returns void

        Optional<ShoppingList> result = shoppingListService.getById(toDeleteId);
        assertTrue(result.isEmpty());
    }


    @Test
    public void testUpdateShoppingListTitle() {
        Optional<ShoppingList> optional = shoppingListService.getById(new ShoppingListId(1L));
        assertTrue(optional.isPresent());

        ShoppingList existing = optional.get();
        ShoppingList updated = new ShoppingList(existing.id(), "Updated Title", existing.shoppingItems(), existing.createdAt(), USER_ID, ProductOrderStrategy.STANDARD);

        ShoppingList saved = shoppingListService.save(updated);

        assertEquals("Updated Title", saved.title());
    }

    @Test
    public void testCheckShoppingItem() {
        Optional<ShoppingList> optional = shoppingListService.getById(new ShoppingListId(1L));
        assertTrue(optional.isPresent());

        ShoppingList original = optional.get();
        ShoppingItem originalItem = original.shoppingItems().getFirst();

        ShoppingItem checkedItem = new ShoppingItem(originalItem.id(), new Product(originalItem.product().name(), originalItem.product().category()), originalItem.quantity(), originalItem.unit(), true, 1);

        ShoppingList modified = new ShoppingList(original.id(), original.title(), List.of(checkedItem), original.createdAt(), USER_ID, ProductOrderStrategy.STANDARD);
        ShoppingList updated = shoppingListService.save(modified);

        assertTrue(updated.shoppingItems().getFirst().checked());
    }




}
