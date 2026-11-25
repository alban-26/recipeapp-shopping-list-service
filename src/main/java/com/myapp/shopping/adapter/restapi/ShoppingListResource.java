package com.myapp.shopping.adapter.restapi;

import com.my.common.api.UserId;
import com.myapp.shopping.adapter.ShoppingItemConverter;
import com.myapp.shopping.adapter.ShoppingListConverter;
import com.myapp.shopping.application.ShoppingListService;
import com.myapp.shopping.domain.model.*;
import io.smallrye.common.annotation.NonBlocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jwk.Use;
import org.openapitools.api.ShoppingListsApi;
import org.openapitools.model.ReorderShoppingItemsRequest;
import org.openapitools.model.ShoppingItemDto;
import org.openapitools.model.ShoppingListDto;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/shoppingLists")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@NonBlocking
@ApplicationScoped
@Slf4j
public class ShoppingListResource implements ShoppingListsApi {

    private final ShoppingListService shoppingListService;
    private final ShoppingListConverter shoppingListConverter;
    private final ShoppingItemConverter shoppingItemConverter;

    @Inject
    JsonWebToken jwt;

    @Inject
    public ShoppingListResource(ShoppingListService shoppingListService,
                                ShoppingListConverter shoppingListConverter,
                                ShoppingItemConverter shoppingItemConverter) {
        this.shoppingListService = shoppingListService;
        this.shoppingListConverter = shoppingListConverter;
        this.shoppingItemConverter = shoppingItemConverter;
    }

    @Override
    public Response createShoppingList(ShoppingListDto dto) {
        String userId = jwt.getSubject();
        log.info("User {} is creating a new shopping list: {}", userId, dto.getTitle());

        // Anlegen heisst immer "neu": mitgeschickte IDs werden ignoriert. Sonst
        // wuerde ein POST mit fremder Listen-ID diese Liste ueberschreiben und
        // eine fremde Eintrags-ID den fremden Eintrag in die neue Liste haengen.
        ShoppingList list = asNew(shoppingListConverter.dtoToDomain(dto)).withUserId(new UserId(userId));
        ShoppingList created = shoppingListService.save(list);

        log.info("User {} created shopping list successfully with id={}", userId, created.id().id());
        return Response.created(URI.create("/shoppingLists/" + created.id().id()))
                .entity(created.id().id())
                .build();
    }

    @Override
    public Response getShoppingListById(Long id) {
        String userId = jwt.getSubject();
        log.info("User {} is fetching shopping list with id={}", userId, id);

        Optional<ShoppingList> listOpt = findOwnList(id, userId);
        if (listOpt.isEmpty()) {
            log.warn("User {} tried to fetch non-existing shopping list with id={}", userId, id);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        log.info("User {} retrieved shopping list successfully with id={}", userId, id);
        ShoppingListDto dto = shoppingListConverter.domainToDto(listOpt.get());
        return Response.ok(dto).build();
    }

    @Override
    public Response getShoppingLists() {
        String userId = jwt.getSubject();
        log.info("User {} is fetching all shopping lists", userId);

        List<ShoppingListDto> allLists = shoppingListService.getAllByUser(new UserId(userId)).stream()
                .map(shoppingListConverter::domainToDto)
                .collect(Collectors.toList());

        if (allLists.isEmpty()) {
            log.info("User {} has no shopping lists", userId);
            return Response.noContent().build();
        }

        log.info("User {} retrieved {} shopping lists", userId, allLists.size());
        return Response.ok(allLists).build();
    }

    @Override
    public Response rearrangeShoppingItems(Long listId, ReorderShoppingItemsRequest reorderShoppingItemsRequest) {
        String userId = jwt.getSubject();
        log.info("User {} is reordering in shopping list with id {}", userId, listId);
        Optional<ShoppingList> list = findOwnList(listId, userId);

        if (list.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        ShoppingList updated =
                shoppingListService.rearrangeItems(
                        list.get(),
                        reorderShoppingItemsRequest.getFromIndex(),
                        reorderShoppingItemsRequest.getToIndex()
                );

        shoppingListService.save(updated.withProductOrderStrategy(ProductOrderStrategy.STANDARD));

        return Response.ok(updated).build();
    }

    @Override
    public Response removeShoppingItem(Long listId, Long itemId) {
        String userId = jwt.getSubject();
        log.info("User {} is removing item id={} from shopping list id={}", userId, itemId, listId);

        if (findOwnList(listId, userId).isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        shoppingListService.removeShoppingItem(new ShoppingListId(listId), new ShoppingItemId(itemId));

        log.info("User {} successfully removed item id={} from shopping list id={}", userId, itemId, listId);
        return Response.noContent().build();
    }

    @Override
    public Response reorderByCommon(Long listId) {
        String userId = jwt.getSubject();
        log.info("User {} is reordering in shopping list with id {}", userId, listId);

        Optional<ShoppingList> list = findOwnList(listId, userId);

        if (list.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        ShoppingList updated = shoppingListService.reorderByCommon(list.get());

        return Response.ok(updated).build();
    }

    @Override
    public Response updateShoppingItem(Long listId, Long itemId, ShoppingItemDto shoppingItemDto) {
        String userId = jwt.getSubject();
        log.info("User {} is updating item id={} in shopping list id={}", userId, itemId, listId);

        if (findOwnList(listId, userId).isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Massgeblich ist die ID aus dem Pfad, nicht die aus dem Body.
        ShoppingItem item = shoppingItemConverter.dtoToDomain(shoppingItemDto).withId(new ShoppingItemId(itemId));

        ShoppingItem updatedItem = shoppingListService.updateShoppingItem(new ShoppingListId(listId), item);

        return Response.ok(shoppingItemConverter.domainToDto(updatedItem)).build();
    }


    @Override
    public Response deleteShoppingList(Long id) {
        String userId = jwt.getSubject();
        log.info("User {} is deleting shopping list with id={}", userId, id);

        Optional<ShoppingList> listOpt = findOwnList(id, userId);
        if (listOpt.isEmpty()) {
            log.warn("User {} tried to delete non-existing shopping list with id={}", userId, id);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        shoppingListService.delete(listOpt.get());
        log.info("User {} deleted shopping list successfully with id={}", userId, id);

        return Response.noContent().build();
    }


    @Override
    public Response updateShoppingList(Long id, ShoppingListDto dto) {
        String userId = jwt.getSubject();
        log.info("User {} is updating shopping list with id={}", userId, id);

        Optional<ShoppingList> existingOpt = findOwnList(id, userId);
        if (existingOpt.isEmpty()) {
            log.warn("User {} tried to update non-existing shopping list with id={}", userId, id);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Massgeblich ist die ID aus dem Pfad, nicht die aus dem Body: geprueft
        // wurde die Liste aus dem Pfad, also wird auch genau diese geaendert.
        ShoppingList updated = shoppingListConverter.dtoToDomain(dto)
                .withId(new ShoppingListId(id))
                .withUserId(new UserId(userId));
        ShoppingList saved = shoppingListService.save(updated);

        log.info("User {} updated shopping list successfully with id={}", userId, id);
        return Response.ok(shoppingListConverter.domainToDto(saved)).build();
    }

    @Override
    public Response addShoppingItem(Long listId, ShoppingItemDto itemDto) {
        String userId = jwt.getSubject();
        log.info("User {} is adding new item to shopping list id={}", userId, listId);

        if (findOwnList(listId, userId).isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Ein neuer Eintrag bekommt immer eine eigene Zeile; eine mitgeschickte
        // ID wuerde sonst einen bestehenden (auch fremden) Eintrag einhaengen.
        ShoppingItem newShoppingItem = shoppingItemConverter.dtoToDomain(itemDto).withId(new ShoppingItemId(0L));
        ShoppingItem created = shoppingListService.addShoppingItem(new ShoppingListId(listId), newShoppingItem);

        log.info("User {} added new item to shopping list id={}", userId, listId);
        return Response.created(URI.create("/shoppingLists/" + listId + "/items"))
                .entity(created.id().id())
                .build();
    }

    /**
     * Liefert die Liste nur, wenn sie dem angemeldeten Nutzer gehoert. Eine
     * fremde Liste wird genauso behandelt wie eine nicht vorhandene (404),
     * damit sich ueber die Antwort nicht herausfinden laesst, welche IDs es gibt.
     */
    private Optional<ShoppingList> findOwnList(Long listId, String userId) {
        if (listId == null || userId == null) {
            return Optional.empty();
        }
        return shoppingListService.getById(new ShoppingListId(listId))
                .filter(list -> list.userId() != null && userId.equals(list.userId().value()));
    }

    private static ShoppingList asNew(ShoppingList list) {
        List<ShoppingItem> items = list.shoppingItems() == null
                ? List.of()
                : list.shoppingItems().stream()
                        .map(item -> item.withId(new ShoppingItemId(0L)))
                        .toList();
        return list.withId(new ShoppingListId(0L)).withShoppingItems(items);
    }


}
