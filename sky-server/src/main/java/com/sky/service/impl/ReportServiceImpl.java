package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ikaros
 */
@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private UserMapper userMapper;
    /**
     * 统计指定时间范围区间的营业额数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放从begin到end的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)){
            //新的Local时间系列会返回一个新的对象，需要将原来的覆盖
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        // org.apache.commons.lang3
        String stringDateList = StringUtils.join(dateList, ",");

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);

            //计算出当天营业额
            Double turnover = ordersMapper.sumByMap(map);
            //当天没有订单时，接收到的值为空，因此要设置为0.0
            turnover = turnover == null ? 0.0 : turnover;
            //将当天营业额加入到营业额列表
            turnoverList.add(turnover);
        }
        String strTurnoverList = StringUtils.join(turnoverList, ",");

        return TurnoverReportVO.builder()
                .dateList(stringDateList)
                .turnoverList(strTurnoverList)
                .build();
    }

    /**
     * 统计指定时间范围内的用户数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        //存放从
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天新增的用户数量
        List<Integer> newUserList = new ArrayList<>();
        //存放每天的总用户数量
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("end", endTime);
            //总用户数量
            Integer count = userMapper.countByMap(map);
            totalUserList.add(count);

            map.put("begin", beginTime);
            //新增用户数量
            count = userMapper.countByMap(map);
            newUserList.add(count);

        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }
}
