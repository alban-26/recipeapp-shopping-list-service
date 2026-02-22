package com.myapp.shopping.domain.model;

public enum ProductCategory {

    // Lebensmittel – Frische
    VEGETABLES(10),
    FRUITS(20),
    BAKERY(30),
    MEAT(40),
    FISH(50),
    DAIRY(60),

    // Lebensmittel – Haltbar
    DRY_GOODS(70),
    SPICES(80),
    SWEETS(90),
    BEVERAGES(100),
    FROZEN(110),

    // Spezialsortimente
    ORGANIC(120),
    VEGAN(130),

    // Allgemein / Non-Food
    HOME_AND_KITCHEN(200),
    BEAUTY(210),
    CLOTHING(220),
    SPORTS(230),
    TOYS(240),
    BOOKS(250),
    ELECTRONICS(260),
    AUTOMOTIVE(270),

    OTHER(999);

    private final int rank;

    ProductCategory(int rank) {
        this.rank = rank;
    }

    /**
     * Returns the ranking value used to sort product categories
     * according to a common in-store ordering (DE / CH).
     * Lower values appear earlier in the assortment.
     */
    public int getRank() {
        return rank;
    }
}
