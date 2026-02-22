package com.myapp.shopping.adapter.database;

import com.myapp.shopping.adapter.database.entities.ShoppingItemEntity;
import com.myapp.shopping.adapter.database.entities.ShoppingListEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShoppingListMapper {

    // ======================
    // Shopping List queries
    // ======================

    @Select("SELECT id, title, created_at, user_id, product_order FROM shopping_list WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "title", column = "title"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "productOrderStrategy", column = "product_order"),
            @Result(property = "items", column = "id",
                    many = @Many(select = "com.myapp.shopping.adapter.database.ShoppingListMapper.findEntriesByShoppingListId"))
    })
    ShoppingListEntity findShoppingListById(Long id);

    @Select("SELECT id, title, created_at, product_order FROM shopping_list")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "title", column = "title"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "productOrderStrategy", column = "product_order"),
            @Result(property = "items", column = "id",
                    many = @Many(select = "com.myapp.shopping.adapter.database.ShoppingListMapper.findEntriesByShoppingListId"))
    })
    List<ShoppingListEntity> findAllShoppingLists();

    @Select("SELECT id, title, created_at, user_id, product_order FROM shopping_list WHERE user_id = #{userId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "title", column = "title"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "productOrderStrategy", column = "product_order"),
            @Result(property = "items", column = "id",
                    many = @Many(select = "com.myapp.shopping.adapter.database.ShoppingListMapper.findEntriesByShoppingListId"))
    })
    List<ShoppingListEntity> findAllShoppingListsByUser(String userId);

    @Insert("INSERT INTO shopping_list (title, created_at, user_id, product_order) VALUES (#{title}, #{createdAt}, #{userId}, #{productOrderStrategy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertShoppingList(ShoppingListEntity entity);

    @Update("UPDATE shopping_list SET title = #{title}, created_at = #{createdAt}, product_order = #{productOrderStrategy} WHERE id = #{id}")
    void updateShoppingList(ShoppingListEntity entity);

    @Delete("DELETE FROM shopping_list WHERE id = #{id}")
    void deleteShoppingList(Long id);

    // ======================
    // Shopping List Entry queries (many-to-many)
    // ======================

    // Fetch all entries (items + list-specific details)
    @Select("""
        SELECT si.id, si.name, si.product_category AS productCategory,
               sle.quantity, sle.unit, sle.checked, sle.rank
        FROM shopping_item si
        JOIN shopping_list_entry sle ON si.id = sle.shopping_item_id
        WHERE sle.shopping_list_id = #{shoppingListId}
        """)
    List<ShoppingItemEntity> findEntriesByShoppingListId(Long shoppingListId);

    // Insert a new shopping item into the catalog
    @Insert("""
        INSERT INTO shopping_item (name, product_category)
        VALUES (#{name}, #{productCategory})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertShoppingItem(ShoppingItemEntity entity);

    // Link item to a shopping list
    @Insert("""
        INSERT INTO shopping_list_entry (shopping_list_id, shopping_item_id, quantity, unit, checked, rank)
        VALUES (#{shoppingListId}, #{itemId}, #{quantity}, #{unit}, #{checked}, #{rank})
        """)
    void linkItemToList(@Param("shoppingListId") Long shoppingListId,
                        @Param("itemId") Long itemId,
                        @Param("quantity") Double quantity,
                        @Param("unit") String unit,
                        @Param("checked") Boolean checked, @Param("rank") int rank);

    // Update list entry details
    @Update("""
        UPDATE shopping_list_entry
        SET quantity = #{quantity}, unit = #{unit}, checked = #{checked}, rank = #{rank}
        WHERE shopping_list_id = #{shoppingListId} AND shopping_item_id = #{itemId}
        """)
    void updateListEntry(@Param("shoppingListId") Long shoppingListId,
                         @Param("itemId") Long itemId,
                         @Param("quantity") Double quantity,
                         @Param("unit") String unit,
                         @Param("checked") Boolean checked,
    @Param("rank") int rank);

    @Update("""
        UPDATE shopping_list_entry
        SET rank = #{rank}
        WHERE shopping_list_id = #{shoppingListId} AND shopping_item_id = #{itemId}
        """)
    void updateRank(@Param("shoppingListId") Long shoppingListId,
                         @Param("itemId") Long itemId,
                         @Param("rank") int rank);

    // Remove item from a shopping list
    @Delete("DELETE FROM shopping_list_entry WHERE shopping_list_id = #{shoppingListId} AND shopping_item_id = #{itemId}")
    void deleteEntryFromList(@Param("shoppingListId") Long shoppingListId,
                             @Param("itemId") Long itemId);

    // Delete item completely from catalog
    @Delete("DELETE FROM shopping_item WHERE id = #{itemId}")
    void deleteShoppingItem(Long itemId);

}
