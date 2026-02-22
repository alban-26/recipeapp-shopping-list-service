package com.myapp.shopping.domain.model;

public enum ProductOrderStrategy {

    /**
     * Orders products according to a user-defined custom sequence.
     */
    STANDARD,

    /**
     * Orders products according to a predefined, store-wide assortment order.
     * Will be ordered by {@link ProductCategory}
     */
    COMMON_ORDER

}
