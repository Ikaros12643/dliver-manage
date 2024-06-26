package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author Ikaros
 */
@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {
    /**
     * 动态条件查询
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 批量插入购物车，用于再来一单接口
     */
    void insertBatch(List<ShoppingCart> shoppingCartList);
}
