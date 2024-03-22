package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ikaros
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //处理各种业务异常(地址簿为空，购物车为空)
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null){
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        ShoppingCart shoppingCart = ShoppingCart
                .builder()
                .userId(BaseContext.getCurrentId())
                .build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size()==0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //2.向订单表插入一条数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);
        order.setUserId(BaseContext.getCurrentId());
        order.setOrderTime(LocalDateTime.now());
        order.setPayStatus(Orders.UN_PAID);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setPhone(addressBook.getPhone());
        order.setConsignee(addressBook.getConsignee());
        //此处mp会自动返回插入的主键值
        ordersMapper.insert(order);


        //3.向订单明细表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart:shoppingCartList){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            //设置当前订单明细所关联的orderId
            orderDetail.setOrderId(order.getId());
            //给orderDetailList插入值
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        //4.清空当前用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        shoppingCartMapper.delete(lqw);

        //封装VO
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderTime(order.getOrderTime())
                .orderAmount(order.getAmount())
                .orderNumber(order.getNumber())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.selectById(userId);

        //调用微信支付接口，生成预支付交易单
/*        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));*/

        OrderPaymentVO orderPaymentVO = new OrderPaymentVO();
        paySuccess(ordersPaymentDTO.getOrderNumber());
        return orderPaymentVO;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = ordersMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        ordersMapper.updateById(orders);
    }

    @Override
    public PageResult historyPage(OrdersPageQueryDTO ordersPageQueryDTO) {
        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ordersPageQueryDTO.getStatus()!=null, Orders::getStatus, ordersPageQueryDTO.getStatus());
        lqw.orderByDesc(Orders::getOrderTime);
        IPage<Orders> page = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        //mp分页查询
        ordersMapper.selectPage(page, lqw);
        //获得分页查询的数据
        List<Orders> ordersList = page.getRecords();
        //封装用于返回的OrderVOList
        List<OrderVO> orderVOList = new ArrayList<>();

        if (ordersList!=null && ordersList.size()>0){
            //遍历orderList给OrderVOList赋值
            ordersList.forEach(order -> {
                LambdaQueryWrapper<OrderDetail> lqw2 = new LambdaQueryWrapper<>();
                lqw2.eq(OrderDetail::getOrderId, order.getId());
                //查询出每个订单对应的订单详情
                List<OrderDetail> orderDetailList = orderDetailMapper.selectList(lqw2);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                orderVOList.add(orderVO);
            });
        }
        return new PageResult(page.getTotal(), orderVOList);
    }

    @Override
    public OrderVO getOrderDetail(Long id) {
        Orders order = ordersMapper.selectById(id);
        LambdaQueryWrapper<OrderDetail> lqw = new LambdaQueryWrapper<>();
        lqw.eq(OrderDetail::getOrderId, id);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(lqw);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    @Override
    public void cancelOrder(Long id) {
        Orders order = Orders.builder()
                .id(id)
                .status(Orders.CANCELLED)
                .build();
        ordersMapper.updateById(order);
    }

    @Override
    public void repetition(Long id) {
        //根据订单id查询对应的订单详情
        LambdaQueryWrapper<OrderDetail> lqw = new LambdaQueryWrapper<>();
        lqw.eq(OrderDetail::getOrderId, id);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(lqw);

        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(orderDetail -> {
            ShoppingCart cart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, cart);
            cart.setUserId(BaseContext.getCurrentId());
            return cart;
        }).collect(Collectors.toList());
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 管理端订单分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        List<Orders> orderList = ordersMapper.conditionSearchPage(ordersPageQueryDTO);
        com.github.pagehelper.Page<Orders> p = (com.github.pagehelper.Page<Orders>) orderList;
//        getO

        return new PageResult(p.getTotal(), p.getResult());
    }

    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO os = ordersMapper.statistics();
        return os;
    }

    /**
     * 商家接单
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        ordersConfirmDTO.setStatus(Orders.CONFIRMED);
        ordersMapper.confirm(ordersConfirmDTO);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        //根据id查询订单
        Orders orderDB = ordersMapper.selectById(ordersRejectionDTO.getId());

        //订单只有存在且状态为2(待接单)才可以拒单
        if (orderDB == null || !orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders order = Orders.builder()
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .status(Orders.CANCELLED)
                .id(ordersRejectionDTO.getId())
                .cancelTime(LocalDateTime.now()).build();

        //用户已支付，需要退款
        Integer payStatus = orderDB.getPayStatus();
        if (payStatus.equals(Orders.PAID)){
            //调用微信支付退款接口
            order.setPayStatus(Orders.REFUND);
            log.info("用户退款");
        }

        //更新数据库 拒单原因 拒单时间
        ordersMapper.updateById(order);
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        Orders orderDB = ordersMapper.selectById(ordersCancelDTO.getId());

        Orders order = Orders.builder()
                .cancelTime(LocalDateTime.now())
                .cancelReason(ordersCancelDTO.getCancelReason())
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED).build();
        if (orderDB.getPayStatus().equals(Orders.PAID)){
            //用户退款
            //调用微信退款api
            order.setPayStatus(Orders.REFUND);
            log.info("给用户退款");
        }

        ordersMapper.updateById(order);
    }

    /**
     * 商家派送订单
     * @param id
     */
    @Override
    public void delivery(Long id) {
        Orders orderDB = ordersMapper.selectById(id);

        //订单只有存在，且订单状态为3（已接单）才可以被派送
        if (orderDB ==null || orderDB.getPayStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders order = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        ordersMapper.updateById(order);
    }

    @Override
    public void complete(Long id) {
        Orders orderDB = ordersMapper.selectById(id);
        //订单只有存在，且订单状态为4（派送中）才可以被完成
        if (orderDB ==null || orderDB.getPayStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders order = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();
        ordersMapper.updateById(order);
    }
}
