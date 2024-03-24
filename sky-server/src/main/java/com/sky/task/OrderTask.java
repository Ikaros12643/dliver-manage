package com.sky.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Ikaros
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrdersMapper ordersMapper;
    @Scheduled(cron = "0 * * * * ?") //每分钟触发一次
//    @Scheduled(cron = "0/5 * * * * ?") //每分钟触发一次
    public void processTimeOutOrder(){
        log.info("定时处理超时订单: {}", LocalDateTime.now());

        // select * from orders where status = 1 and order_time < (当前时间-15min)
        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Orders::getStatus, Orders.PENDING_PAYMENT);
        lqw.lt(Orders::getOrderTime, LocalDateTime.now().minusMinutes(15));
        List<Orders> orderList = ordersMapper.selectList(lqw);
        if (orderList != null && orderList.size() > 0){
            for (Orders order : orderList){
                order.setCancelTime(LocalDateTime.now());
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单支付超时，自动取消");
                ordersMapper.updateById(order);
            }
        }

    }

    /**
     * 处理一直处于派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        log.info("处理处理处于派送中的订单: {}", LocalDateTime.now());
        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS);
        List<Orders> orderList = ordersMapper.selectList(lqw);
        if (orderList != null && orderList.size() > 0){
            for (Orders order : orderList){
                order.setStatus(Orders.COMPLETED);
                ordersMapper.updateById(order);
            }
        }
    }
}
