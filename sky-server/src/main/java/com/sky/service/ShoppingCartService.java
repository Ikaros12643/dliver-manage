package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

/**
 * @author Ikaros
 */
public interface ShoppingCartService{
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> showShoppingCart();

    void cleanCart();

    void subItem(ShoppingCartDTO shoppingCartDTO);
}
