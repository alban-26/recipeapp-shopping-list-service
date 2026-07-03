package com.myapp.shopping.adapter;

import com.my.common.api.UserId;
import com.myapp.shopping.adapter.database.entities.ShoppingItemEntity;
import com.myapp.shopping.adapter.database.entities.ShoppingListEntity;
import com.myapp.shopping.adapter.messaging.MealPlanCreatedEvent;
import com.myapp.shopping.adapter.messaging.RecipeIngredient;
import com.myapp.shopping.domain.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.apache.commons.lang3.NotImplementedException;
import org.openapitools.model.ShoppingItemDto;
import org.openapitools.model.ShoppingListDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ApplicationScoped
public class ShoppingListConverterImpl implements ShoppingListConverter {

    private final ShoppingItemConverter shoppingItemConverter;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMAN);


    @Inject
    public ShoppingListConverterImpl(ShoppingItemConverter shoppingItemConverter) {
        this.shoppingItemConverter = shoppingItemConverter;
    }

    @Override
    public ShoppingList dtoToDomain(ShoppingListDto shoppingListDto) {
        return new ShoppingList(new ShoppingListId(createId(shoppingListDto.getId())), shoppingListDto.getTitle(), shoppingListDto.getShoppingItems().stream().map(shoppingItemConverter::dtoToDomain).toList(), shoppingListDto.getCreatedAt(), null, ProductOrderStrategy.valueOf(shoppingListDto.getOrderStrategy().name()));
    }

    @Override
    public ShoppingList entityToDomain(ShoppingListEntity shoppingListEntity) {
        return new ShoppingList(new ShoppingListId(shoppingListEntity.getId()), shoppingListEntity.getTitle(), shoppingListEntity.getItems().stream().map(shoppingItemConverter::entityToDomain).toList(), shoppingListEntity.getCreatedAt(), new UserId(shoppingListEntity.getUserId()), ProductOrderStrategy.valueOf(shoppingListEntity.getProductOrderStrategy()));
    }

    @Override
    public ShoppingListDto domainToDto(ShoppingList shoppingList) {
        return new ShoppingListDto(shoppingList.id().id(), shoppingList.title(), org.openapitools.model.ProductOrderStrategy.fromString(shoppingList.productOrderStrategy().name()), createShoppingItems(shoppingList.shoppingItems(), shoppingList.productOrderStrategy()), shoppingList.createdAt());
    }

    private List<ShoppingItemDto> createShoppingItems(
            List<ShoppingItem> shoppingItems,
            ProductOrderStrategy productOrderStrategy
    ) {
        Stream<ShoppingItem> stream = shoppingItems.stream();

        if (productOrderStrategy == ProductOrderStrategy.COMMON_ORDER) {
            stream = stream.sorted(
                    Comparator
                            .comparingInt((ShoppingItem item) ->
                                    item.product().category().getRank()
                            )
                            .thenComparingInt(ShoppingItem::rank)
            );
        } else {
            stream = stream.sorted(Comparator.comparingInt(ShoppingItem::rank));
        }

        return stream
                .map(shoppingItemConverter::domainToDto)
                .toList();
    }


    @Override
    public ShoppingListEntity domainToEntity(ShoppingList shoppingList) {
        List<ShoppingItemEntity> entities = shoppingList.shoppingItems().stream().map(shoppingItemConverter::domainToEntity).toList();
        return new ShoppingListEntity(shoppingList.id().id(), shoppingList.title(), shoppingList.createdAt(), shoppingList.userId().value(), shoppingList.productOrderStrategy().name(), entities);
    }
    public static LocalDate convertDateToLocalDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }


    @Override
    public ShoppingList eventToDomain(MealPlanCreatedEvent mealPlanCreatedEvent) {

        List<RecipeIngredient> ingredients =
                new ArrayList<>(mealPlanCreatedEvent.getRecipeIngredients());
        List<ShoppingItem> shoppingItems;
         shoppingItems =
                IntStream.range(0, ingredients.size())
                        .mapToObj(i ->
                                createShoppingItem(
                                        ingredients.get(i),
                                        i // ← rank
                                )
                        )
                        .toList();
        return new ShoppingList(
                new ShoppingListId(0L),
                createShoppingListTitle(mealPlanCreatedEvent.getStartDate(), mealPlanCreatedEvent.getEndDate()),
                shoppingItems,
                LocalDate.now(),
                mealPlanCreatedEvent.getUserId(),
                ProductOrderStrategy.STANDARD
        );
    }

    private static String createShoppingListTitle(LocalDate start, LocalDate end) {
        if (start.equals(end)) {
            return start.format(FORMATTER);
        }
        return start.format(FORMATTER) + " - " + end.format(FORMATTER);
    }


    private static ShoppingItem createShoppingItem(
            RecipeIngredient recipeIngredient,
            int rank
    ) {
        return new ShoppingItem(
                new ShoppingItemId(0L),
                new Product(
                        recipeIngredient.ingredient().name(),
                        recipeIngredient.ingredient().productCategory()
                ),
                recipeIngredient.quantity(),
                recipeIngredient.unit(),
                false,
                rank
        );
    }


    @Override
    public MealPlanCreatedEvent domainToEvent(ShoppingList shoppingList) {
        throw new NotImplementedException("Mapping ShoppingList → MealPlanCreatedEvent is not implemented yet since no publishing is required.");
    }
}
