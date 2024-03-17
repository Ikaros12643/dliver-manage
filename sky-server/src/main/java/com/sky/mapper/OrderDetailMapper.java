package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Ikaros
 */
@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {
    void insertBatch(@Param("orderDetailList") List<OrderDetail> orderDetailList);

}
