package com.myapp.shopping.adapter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.MessageConverter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.lang.reflect.Type;

@ApplicationScoped
public class MealPlanDeserializer implements MessageConverter {

    @Inject
    ObjectMapper mapper;

    @Override
    public boolean canConvert(Message<?> in, Type target) {
        return target.equals(MealPlanCreatedEvent.class);
    }

    @Override
    public Message<?> convert(Message<?> in, Type target) {
        try {
            byte[] payload = in.getPayload().toString().getBytes();
            MealPlanCreatedEvent dto = mapper.readValue(payload, MealPlanCreatedEvent.class);
            return in.withPayload(dto);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize RecipeInfoDto", e);
        }
    }
}
