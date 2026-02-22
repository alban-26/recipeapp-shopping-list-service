package com.myapp.shopping.adapter.messaging;


import com.myapp.shopping.adapter.ShoppingListConverter;
import com.myapp.shopping.application.ShoppingListService;
import com.myapp.shopping.domain.model.ShoppingList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class MealPlanCreatedConsumer {

    private final ShoppingListService shoppingListService;
    private final ShoppingListConverter shoppingListConverter;



    @Inject
    public MealPlanCreatedConsumer(ShoppingListService shoppingListService, ShoppingListConverter shoppingListConverter) {

        this.shoppingListService = shoppingListService;
        this.shoppingListConverter = shoppingListConverter;
    }

    @Incoming("recipe-ingredients")
    public void consume(MealPlanCreatedEvent event) {
        System.out.println("📩 Received MealPlanCreatedEvent: ");

        ShoppingList shoppingList = shoppingListConverter.eventToDomain(event).withUserId(event.userId);

        shoppingListService.save(shoppingList);
    }
}
