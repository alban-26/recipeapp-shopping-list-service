package com.myapp.shopping.adapter.restapi;

import com.my.common.api.UserId;
import com.myapp.shopping.adapter.ShoppingItemConverter;
import com.myapp.shopping.adapter.ShoppingListConverter;
import com.myapp.shopping.application.ShoppingListService;
import com.myapp.shopping.domain.model.*;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapitools.model.ShoppingItemDto;
import org.openapitools.model.ShoppingListDto;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

@QuarkusTest
class ShoppingListResourceTest {

    @InjectMock
    ShoppingListService shoppingListService;

    @InjectMock
    ShoppingListConverter shoppingListConverter;

    @InjectMock
    ShoppingItemConverter shoppingItemConverter;

    @InjectMock
    JsonWebToken jwt;

    // --- GET /shoppingLists ---
    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void testGetShoppingLists_returnsList() {
        Mockito.when(jwt.getSubject()).thenReturn("user1");

        ShoppingList list = new ShoppingList(
                new ShoppingListId(1L),
                "Groceries",
                List.of(),
                LocalDate.of(2025, 1, 1),
                new UserId("user1"),
                ProductOrderStrategy.STANDARD
        );

        ShoppingListDto dto = new ShoppingListDto();
        dto.setId(1L);
        dto.setTitle("Groceries");

        Mockito.when(shoppingListService.getAllByUser(new UserId("user1")))
                .thenReturn(List.of(list));
        Mockito.when(shoppingListConverter.domainToDto(list)).thenReturn(dto);

        given()
                .accept(ContentType.JSON)
                .when()
                .get("/shoppingLists")
                .then()
                .statusCode(200)
                .body("[0].id", equalTo(1))
                .body("[0].title", equalTo("Groceries"));
    }

    // --- POST /shoppingLists ---
    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void testCreateShoppingList_createsSuccessfully() {
        Mockito.when(jwt.getSubject()).thenReturn("user1");

        ShoppingListDto inputDto = new ShoppingListDto();
        inputDto.setTitle("Weekly Shopping");

        ShoppingList domain = new ShoppingList(
                new ShoppingListId(1L),
                "Weekly Shopping",
                List.of(),
                LocalDate.of(2025, 1, 1),
                new UserId("user1"),
                ProductOrderStrategy.STANDARD
        );

        ShoppingList saved = new ShoppingList(
                new ShoppingListId(1L),
                "Weekly Shopping",
                List.of(),
                LocalDate.of(2025, 1, 1),
                new UserId("user1"),
                ProductOrderStrategy.STANDARD
        );

        Mockito.when(shoppingListConverter.dtoToDomain(any(ShoppingListDto.class))).thenReturn(domain);
        Mockito.when(shoppingListService.save(any(ShoppingList.class))).thenReturn(saved);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(inputDto)
                .when()
                .post("/shoppingLists")
                .then()
                .statusCode(201)
                .header("Location", endsWith("/shoppingLists/1"))
                .body(equalTo("1"));
    }

    // --- GET /shoppingLists/{id} ---
    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void testGetShoppingListById_returnsList() {
        Mockito.when(jwt.getSubject()).thenReturn("user1");

        ShoppingList list = new ShoppingList(
                new ShoppingListId(1L),
                "Groceries",
                List.of(),
                LocalDate.of(2025, 1, 1),
                new UserId("user1"),
                ProductOrderStrategy.STANDARD
        );

        ShoppingListDto dto = new ShoppingListDto();
        dto.setId(1L);
        dto.setTitle("Groceries");

        Mockito.when(shoppingListService.getById(new ShoppingListId(1L)))
                .thenReturn(Optional.of(list));
        Mockito.when(shoppingListConverter.domainToDto(list)).thenReturn(dto);

        given()
                .accept(ContentType.JSON)
                .when()
                .get("/shoppingLists/1")
                .then()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("title", equalTo("Groceries"));
    }

    // --- DELETE /shoppingLists/{id} ---
    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void testDeleteShoppingList() {
        Mockito.when(jwt.getSubject()).thenReturn("user1");

        ShoppingList list = new ShoppingList(
                new ShoppingListId(1L),
                "Groceries",
                List.of(),
                LocalDate.of(2025, 1, 1),
                new UserId("user1"),
                ProductOrderStrategy.STANDARD
        );

        Mockito.when(shoppingListService.getById(new ShoppingListId(1L)))
                .thenReturn(Optional.of(list));

        given()
                .accept(ContentType.JSON)
                .when()
                .delete("/shoppingLists/1")
                .then()
                .statusCode(204);
    }

    // --- DELETE /shoppingLists/{listId}/items/{itemId} ---
    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void testRemoveShoppingItem() {
        Mockito.when(jwt.getSubject()).thenReturn("user1");

        given()
                .accept(ContentType.JSON)
                .when()
                .delete("/shoppingLists/1/shoppingItems/10")
                .then()
                .statusCode(204);

        Mockito.verify(shoppingListService).removeShoppingItem(
                new ShoppingListId(1L),
                new ShoppingItemId(10L)
        );
    }

    // --- POST /shoppingLists/{listId}/items ---
    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void testAddShoppingItem() {
        Mockito.when(jwt.getSubject()).thenReturn("user1");

        ShoppingItemDto itemDto = new ShoppingItemDto();

        ShoppingItem item = new ShoppingItem(
                new ShoppingItemId(10L),
                new Product("Milk", ProductCategory.BAKERY),
                1,
                Unit.BUNCH,
                true,
                1
        );

        Mockito.when(shoppingItemConverter.dtoToDomain(any(ShoppingItemDto.class))).thenReturn(item);
        Mockito.when(shoppingListService.addShoppingItem(any(), any())).thenReturn(item);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(itemDto)
                .when()
                .post("/shoppingLists/1/shoppingItems")
                .then()
                .statusCode(201)
                .header("Location", endsWith("/shoppingLists/1/items"))
                .body(equalTo("10"));
    }

}
