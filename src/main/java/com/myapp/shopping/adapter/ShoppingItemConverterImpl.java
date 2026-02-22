package com.myapp.shopping.adapter;

import com.myapp.shopping.adapter.database.entities.ShoppingItemEntity;
import com.myapp.shopping.domain.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.openapitools.model.ProductDto;
import org.openapitools.model.ShoppingItemDto;

@ApplicationScoped
public class ShoppingItemConverterImpl implements ShoppingItemConverter {

    @Override
    public ShoppingItem dtoToDomain(ShoppingItemDto shoppingItemDto) {
        return new ShoppingItem(new ShoppingItemId(createId(shoppingItemDto.getId())),
                createProduct(shoppingItemDto.getProduct()), shoppingItemDto.getQuantity(),
                Unit.fromAbbreviation(shoppingItemDto.getUnit()), shoppingItemDto.getChecked(), shoppingItemDto.getRank());
    }

    private static Product createProduct(ProductDto productDto) {
        ProductDto.CategoryEnum dtoCategory = productDto.getCategory();
        ProductCategory category = dtoCategory != null
                ? ProductCategory.valueOf(dtoCategory.name())
                : ProductCategory.OTHER;

        return new Product(productDto.getName(), category);
    }


    @Override
    public ShoppingItem entityToDomain(ShoppingItemEntity shoppingItemEntity) {
        return new ShoppingItem(new ShoppingItemId(shoppingItemEntity.getId()),
                new Product(shoppingItemEntity.getName(), ProductCategory.valueOf(shoppingItemEntity.getProductCategory())),
                shoppingItemEntity.getQuantity(), Unit.valueOf(shoppingItemEntity.getUnit()), shoppingItemEntity.getChecked(), shoppingItemEntity.getRank()
        );
    }

    @Override
    public ShoppingItemDto domainToDto(ShoppingItem shoppingItem) {
        Product product = shoppingItem.product();
        ProductDto.CategoryEnum dtoCategory = product.category() != null
                ? ProductDto.CategoryEnum.valueOf(product.category().name())
                : ProductDto.CategoryEnum.OTHER;

        ProductDto productDto = new ProductDto()
                .name(product.name())
                .category(dtoCategory);

        return new ShoppingItemDto()
                .id(shoppingItem.id().id())
                .product(productDto)
                .quantity(shoppingItem.quantity())
                .unit(shoppingItem.unit().getAbbreviation())
                .checked(shoppingItem.checked())
                .rank(shoppingItem.rank());
    }

    @Override
    public ShoppingItemEntity domainToEntity(ShoppingItem shoppingItem) {
        ShoppingItemEntity shoppingItemEntity = new ShoppingItemEntity();

        shoppingItemEntity.setId(shoppingItem.id().id());
        shoppingItemEntity.setName(shoppingItem.product().name());
        shoppingItemEntity.setProductCategory(shoppingItem.product().category().name());
        shoppingItemEntity.setQuantity(shoppingItem.quantity());
        shoppingItemEntity.setUnit(shoppingItem.unit().name());
        shoppingItemEntity.setChecked(shoppingItem.checked());
        shoppingItemEntity.setRank(shoppingItem.rank());

        return shoppingItemEntity;
    }


}
