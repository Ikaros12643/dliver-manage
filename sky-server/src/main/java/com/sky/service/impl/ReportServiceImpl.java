package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ikaros
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;
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

    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        //存放从begin到end的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //遍历dateList集合，查询每天的有效订单数和订单总数
        List<Double> orderCountList = new ArrayList<>();
        List<Double> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            Map map = new HashMap();
            map.put("date", date);
            //查询订单总数
            Double dayTotalOrders = ordersMapper.getDailyOrders(map);
            orderCountList.add(dayTotalOrders);

            //查询有效订单数
            map.put("status", Orders.COMPLETED);
            Double dayValidOrders = ordersMapper.getDailyOrders(map);
            validOrderCountList.add(dayValidOrders);
        }
        //订单总数
        Double totalOrderCount = orderCountList.stream().reduce(Double::sum).get();
        //查询有效订单数
        Double validOrderCount = validOrderCountList.stream().reduce(Double::sum).get();

        Double rate = validOrderCount/totalOrderCount;

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .totalOrderCount(totalOrderCount.intValue())
                .validOrderCount(validOrderCount.intValue())
                .orderCompletionRate(rate)
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .build();
    }

    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        List<GoodsSalesDTO> goodsSales = ordersMapper.getSalesTop10(begin, end);
        List<String> names = goodsSales.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = goodsSales.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        //封装返回结果数据
        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(names, ","))
                .numberList(StringUtils.join(numbers, ","))
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response){
        //1.查询数据库，获取营业数据
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate begin = end.minusDays(30);
        BusinessDataVO businessData = getBusinessData(begin, end);
        //2.通过POI将数据写入到Excel文件
//        InputStream resourceStream = this.getClass().getClassLoader().getResourceAsStream("classpath:template/运营数据报表模板.xlsx");
        XSSFWorkbook excel = null;
        try {
            File file = ResourceUtils.getFile("classpath:template/运营数据报表模板.xlsx");

            //基于模板文件创建一个新的excel文件
            excel = new XSSFWorkbook(file);

            //填充数据--时间
            XSSFSheet sheet1 = excel.getSheetAt(0);
            //获取第二行的第二个单元格，并填充时间
            sheet1.getRow(1).getCell(1).setCellValue("时间: " + begin + "至" + end);
            //获得第四行
            XSSFRow row4 = sheet1.getRow(3);
            row4.getCell(2).setCellValue(businessData.getTurnover());
            row4.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row4.getCell(6).setCellValue(businessData.getNewUsers());
            //获得第五行
            XSSFRow row5 = sheet1.getRow(4);
            row5.getCell(2).setCellValue(businessData.getValidOrderCount());
            row5.getCell(4).setCellValue(businessData.getUnitPrice());


            //2.填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO dailyBusinessData = workspaceService.getBusinessData(date);
                XSSFRow row8 = sheet1.getRow(7 + i);
                row8.getCell(1).setCellValue(date.toString());
                row8.getCell(2).setCellValue(dailyBusinessData.getTurnover());
                row8.getCell(3).setCellValue(dailyBusinessData.getValidOrderCount());
                row8.getCell(4).setCellValue(dailyBusinessData.getOrderCompletionRate());
                row8.getCell(5).setCellValue(dailyBusinessData.getUnitPrice());
                row8.getCell(6).setCellValue(dailyBusinessData.getNewUsers());
            }


            //3.通过输出流将Excel文件下载到浏览器
            ServletOutputStream output = response.getOutputStream();
            excel.write(output);

            excel.close();
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
            log.info("POI错误");
        }

    }

    public BusinessDataVO getBusinessData(LocalDate begin, LocalDate end){
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        Integer totalOrderCount = ordersMapper.countByMap(map);
        map.put("status", Orders.COMPLETED);
        //营业额
        Double turnover = ordersMapper.sumByMap(map);
        turnover = turnover == null? 0.0 : turnover;

        //有效订单数
        Integer validOrderCount = ordersMapper.countByMap(map);
        Double unitPrice = 0.0;

        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0 && validOrderCount != 0){
            //订单完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            //平均客单价
            unitPrice = turnover / validOrderCount;
        }

        //新增用户数
        Integer newUsers = userMapper.countByMap(map);

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }
}
