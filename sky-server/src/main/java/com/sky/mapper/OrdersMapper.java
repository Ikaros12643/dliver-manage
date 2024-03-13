package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Ikaros
 */
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {
}
