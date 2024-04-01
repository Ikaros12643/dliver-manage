package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @author Ikaros
 */
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {
    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    List<Orders> conditionSearchPage(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("""
            select\s
            sum(case when `status` = 2 then 1 else 0 end) as `TO_BE_CONFIRMED`,\s
            sum(case when `status` = 3 then 1 else 0 end) as `CONFIRMED`,\s
            sum(case when `status` = 4 then 1 else 0 end) as `DELIVERY_IN_PROGRESS`\s
            from orders;""")
    OrderStatisticsVO statistics();

    @Update("update orders set status = #{status} where id = #{id}")
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 根据动态条件统计营业额数据
     * @param map
     * @return
     */
    Double sumByMap(Map map);

    /**
     * 根据动态条件统计每日订单数量
     * @param map
     * @return
     */
    Double getDailyOrders(Map map);

    @Select("""
               select od.name as name, sum(od.number) as number from order_detail od, orders o\s
               where od.order_id=o.id\s
               and o.`status` = 5\s
               and date(o.order_time) BETWEEN #{begin} and #{end}\s
               GROUP BY od.name\s
               ORDER BY sum(od.number) desc\s
               limit 0,10;""")
    List<GoodsSalesDTO> getSalesTop10(LocalDate begin, LocalDate end);

    Integer countByMap(Map map);
}
