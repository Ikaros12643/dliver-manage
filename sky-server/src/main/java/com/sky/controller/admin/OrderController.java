package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author Ikaros
 */
@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;
    @GetMapping("/conditionSearch")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("商家订单搜索");
        PageResult pr = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pr);
    }

    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> statistics(){
        log.info("各个状态的订单数量查询");
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * 商家查询订单详情
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id){
        log.info("查询订单详情{}", id);
        OrderVO orderVO = orderService.getOrderDetail(id);
        return Result.success(orderVO);
    }

    /**
     * 商家接单
     * @param ordersConfirmDTO
     * @return
     */
    @PutMapping("/confirm")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
        log.info("商家接单：{}", ordersConfirmDTO);
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * 商家拒单
     * @param ordersRejectionDTO
     * @return
     */
    @PutMapping("/rejection")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO){
        log.info("商家拒单：{}", ordersRejectionDTO);
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     * @return
     */
    @PutMapping("/cancel")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO){
        log.info("商家取消订单：{}", ordersCancelDTO);
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }

    /**
     * 派送订单
     * @param id
     * @return
     */
    @PutMapping("/delivery/{id}")
    public Result delivery(@PathVariable Long id){
        log.info("派送订单: {}", id);
        orderService.delivery(id);
        return Result.success();
    }

    @PutMapping("/complete/{id}")
    public Result complete(@PathVariable Long id){
        log.info("完成订单：{}", id);
        orderService.complete(id);
        return Result.success();
    }
}
