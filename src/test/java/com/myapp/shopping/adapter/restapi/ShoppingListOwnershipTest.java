package com.myapp.shopping.adapter.restapi;

import com.my.common.api.UserId;
import com.myapp.shopping.application.EmbeddedPostgresResource;
import com.myapp.shopping.application.ShoppingListService;
import com.myapp.shopping.domain.model.ShoppingItem;
import com.myapp.shopping.domain.model.ShoppingList;
import com.myapp.shopping.domain.model.ShoppingListId;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueft ueber die echte REST-Schnittstelle und die echte Datenbank, dass
 * Einkaufslisten und ihre Eintraege nur fuer den Besitzer erreichbar sind.
 *
 * Testdaten (V2__insert_shopping_list_data.sql): Liste 1 "Test Shopping List"
 * gehoert Nutzer "1" und enthaelt Eggs (1), Milk (3) und Butter (5).
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTestResource(EmbeddedPostgresResource.class)
class ShoppingListOwnershipTest {

    private static final String OWNER = "1";
    private static final String STRANGER = "2";
    private static final long LIST = 1L;
    private static final long EGGS = 1L;

    @InjectMock
    JsonWebToken jwt;

    @Inject
    ShoppingListService shoppingListService;

    @Inject
    Flyway flyway;

    @BeforeEach
    void setupDatabase() {
        flyway.clean();
        flyway.migrate();
    }

    @AfterAll
    void cleanupAfterTests() {
        flyway.clean();
    }

    private void actAs(String userId) {
        Mockito.when(jwt.getSubject()).thenReturn(userId);
    }

    /** Derselbe Aufbau, den die App schickt. */
    private static Map<String, Object> listJson(long id, String title, List<Map<String, Object>> items) {
        return Map.of(
                "id", id,
                "title", title,
                "shoppingItems", items,
                "createdAt", "2026-01-05",
                "orderStrategy", "STANDARD");
    }

    private static Map<String, Object> itemJson(long id, String name, double quantity) {
        return Map.of(
                "id", id,
                "product", Map.of("name", name, "category", "BAKERY"),
                "quantity", quantity,
                "unit", "g",
                "checked", false,
                "rank", 9);
    }

    private ShoppingList stored(long id) {
        return shoppingListService.getById(new ShoppingListId(id)).orElseThrow();
    }

    private ShoppingItem storedItem(long listId, long itemId) {
        return stored(listId).shoppingItems().stream()
                .filter(item -> item.id().id() == itemId)
                .findFirst()
                .orElseThrow();
    }

    private List<Long> idsByRank(long listId) {
        return stored(listId).shoppingItems().stream()
                .sorted(Comparator.comparingInt(ShoppingItem::rank))
                .map(item -> item.id().id())
                .toList();
    }

    private long createList(String userId, Map<String, Object> body) {
        actAs(userId);
        return Long.parseLong(given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/shoppingLists")
                .then()
                .statusCode(201)
                .extract().asString().trim());
    }

    // ------------------------------------------------------------------
    // Liste lesen, umbenennen, loeschen
    // ------------------------------------------------------------------

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void ownerCanReadOwnList() {
        actAs(OWNER);

        given()
                .accept(ContentType.JSON)
                .when()
                .get("/shoppingLists/" + LIST)
                .then()
                .statusCode(200)
                .body("title", equalTo("Test Shopping List"))
                .body("shoppingItems.size()", equalTo(3));
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void strangerCannotReadList() {
        actAs(STRANGER);

        given()
                .accept(ContentType.JSON)
                .when()
                .get("/shoppingLists/" + LIST)
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void ownerCanRenameOwnList() {
        actAs(OWNER);

        given()
                .contentType(ContentType.JSON)
                .body(listJson(LIST, "Wocheneinkauf", List.of()))
                .when()
                .put("/shoppingLists/" + LIST)
                .then()
                .statusCode(200)
                .body("title", equalTo("Wocheneinkauf"));

        ShoppingList list = stored(LIST);
        assertEquals("Wocheneinkauf", list.title());
        assertEquals(OWNER, list.userId().value());
        assertEquals(3, list.shoppingItems().size(), "Umbenennen darf keine Eintraege verlieren");
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void strangerCannotRenameList() {
        actAs(STRANGER);

        given()
                .contentType(ContentType.JSON)
                .body(listJson(LIST, "Gekapert", List.of()))
                .when()
                .put("/shoppingLists/" + LIST)
                .then()
                .statusCode(404);

        assertEquals("Test Shopping List", stored(LIST).title());
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void renameUsesIdFromPathNotFromBody() {
        long own = createList(STRANGER, listJson(0, "Meine Liste", List.of()));

        // Eigene Liste im Pfad, fremde ID im Body: geaendert wird die eigene.
        given()
                .contentType(ContentType.JSON)
                .body(listJson(LIST, "Gekapert", List.of()))
                .when()
                .put("/shoppingLists/" + own)
                .then()
                .statusCode(200);

        assertEquals("Test Shopping List", stored(LIST).title());
        assertEquals(OWNER, stored(LIST).userId().value());
        assertEquals("Gekapert", stored(own).title());
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void createNeverOverwritesExistingList() {
        // POST mit fremder Listen-ID und fremder Eintrags-ID.
        long created = createList(STRANGER, listJson(LIST, "Gekapert", List.of(itemJson(EGGS, "Zucker", 1))));

        assertNotEquals(LIST, created, "POST muss eine neue Liste anlegen");
        ShoppingList original = stored(LIST);
        assertEquals("Test Shopping List", original.title());
        assertEquals(OWNER, original.userId().value());
        assertEquals("Eggs", storedItem(LIST, EGGS).product().name());

        ShoppingList own = stored(created);
        assertEquals(STRANGER, own.userId().value());
        assertEquals(1, own.shoppingItems().size());
        ShoppingItem ownItem = own.shoppingItems().getFirst();
        assertNotEquals(EGGS, ownItem.id().id(), "Der Eintrag muss eine eigene Zeile bekommen");
        assertEquals("Zucker", ownItem.product().name());
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void strangerCannotDeleteList() {
        actAs(STRANGER);

        given()
                .when()
                .delete("/shoppingLists/" + LIST)
                .then()
                .statusCode(404);

        assertEquals(3, stored(LIST).shoppingItems().size());
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void ownerCanDeleteOwnList() {
        actAs(OWNER);

        given()
                .when()
                .delete("/shoppingLists/" + LIST)
                .then()
                .statusCode(204);

        assertTrue(shoppingListService.getById(new ShoppingListId(LIST)).isEmpty());
    }

    // ------------------------------------------------------------------
    // Eintraege
    // ------------------------------------------------------------------

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void strangerCannotAddItem() {
        actAs(STRANGER);

        given()
                .contentType(ContentType.JSON)
                .body(itemJson(0, "Zucker", 1))
                .when()
                .post("/shoppingLists/" + LIST + "/shoppingItems")
                .then()
                .statusCode(404);

        assertEquals(3, stored(LIST).shoppingItems().size());
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void strangerCannotUpdateItem() {
        actAs(STRANGER);

        given()
                .contentType(ContentType.JSON)
                .body(itemJson(EGGS, "Gekapert", 99))
                .when()
                .patch("/shoppingLists/" + LIST + "/shoppingItems/" + EGGS)
                .then()
                .statusCode(404);

        ShoppingItem eggs = storedItem(LIST, EGGS);
        assertEquals("Eggs", eggs.product().name());
        assertEquals(12.0, eggs.quantity());
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void strangerCannotRemoveItem() {
        actAs(STRANGER);

        given()
                .when()
                .delete("/shoppingLists/" + LIST + "/shoppingItems/" + EGGS)
                .then()
                .statusCode(404);

        assertEquals(3, stored(LIST).shoppingItems().size());
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void strangerCannotReorder() {
        List<Long> before = idsByRank(LIST);
        actAs(STRANGER);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("fromIndex", 0, "toIndex", 2))
                .when()
                .post("/shoppingLists/" + LIST + "/shoppingItems/reorder")
                .then()
                .statusCode(404);

        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/shoppingLists/" + LIST + "/shoppingItems/reorderByCommon")
                .then()
                .statusCode(404);

        assertEquals(before, idsByRank(LIST));
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void foreignItemCannotBeLinkedIntoOwnList() {
        long own = createList(STRANGER, listJson(0, "Meine Liste", List.of()));

        // Eintrag mit der ID eines fremden Eintrags hinzufuegen ...
        long added = Long.parseLong(given()
                .contentType(ContentType.JSON)
                .body(itemJson(EGGS, "Zucker", 1))
                .when()
                .post("/shoppingLists/" + own + "/shoppingItems")
                .then()
                .statusCode(201)
                .extract().asString().trim());
        assertNotEquals(EGGS, added, "Der Eintrag muss eine eigene Zeile bekommen");

        // ... und der Versuch, den fremden Eintrag ueber die eigene Liste zu aendern,
        // darf am Original nichts aendern.
        given()
                .contentType(ContentType.JSON)
                .body(itemJson(EGGS, "Gekapert", 99))
                .when()
                .patch("/shoppingLists/" + own + "/shoppingItems/" + EGGS)
                .then()
                .statusCode(200);

        ShoppingItem eggs = storedItem(LIST, EGGS);
        assertEquals("Eggs", eggs.product().name());
        assertEquals(12.0, eggs.quantity());
        assertEquals("Zucker", storedItem(own, added).product().name());
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void ownerCanStillEditOwnItems() {
        actAs(OWNER);

        long added = Long.parseLong(given()
                .contentType(ContentType.JSON)
                .body(itemJson(0, "Zucker", 500))
                .when()
                .post("/shoppingLists/" + LIST + "/shoppingItems")
                .then()
                .statusCode(201)
                .extract().asString().trim());
        assertEquals(4, stored(LIST).shoppingItems().size());

        given()
                .contentType(ContentType.JSON)
                .body(itemJson(added, "Rohrzucker", 250))
                .when()
                .patch("/shoppingLists/" + LIST + "/shoppingItems/" + added)
                .then()
                .statusCode(200);
        ShoppingItem sugar = storedItem(LIST, added);
        assertEquals("Rohrzucker", sugar.product().name());
        assertEquals(250.0, sugar.quantity());

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("fromIndex", 0, "toIndex", 2))
                .when()
                .post("/shoppingLists/" + LIST + "/shoppingItems/reorder")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/shoppingLists/" + LIST + "/shoppingItems/reorderByCommon")
                .then()
                .statusCode(200);

        given()
                .when()
                .delete("/shoppingLists/" + LIST + "/shoppingItems/" + added)
                .then()
                .statusCode(204);
        assertEquals(3, stored(LIST).shoppingItems().size());
    }

    @Test
    @TestSecurity(user = "user1", roles = {"user"})
    void listOverviewContainsOnlyOwnLists() {
        createList(STRANGER, listJson(0, "Meine Liste", List.of()));

        List<String> titles = given()
                .accept(ContentType.JSON)
                .when()
                .get("/shoppingLists")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("title", String.class);

        assertEquals(List.of("Meine Liste"), titles);
        assertEquals(2, shoppingListService.getAllByUser(new UserId(OWNER)).size());
    }
}
