package com.sky.controller.admin;

import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

/**
 * @author Ikaros
 */
@RestController("adminShopController")
@Slf4j
@RequestMapping("/admin/shop")
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;

    private static final String key = "SHOP_STATUS";
    /**
     * 设置店铺的营业状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置店铺的营业状态: {}", status == 1 ? "营业中":"打烊");
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set(key, status);

        return Result.success();
    }

    @GetMapping("/status")
    public Result<Integer> getStatus(){
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(key);
        log.info("获取到店铺的营业状态为: {}", shopStatus == 1 ? "营业中":"打烊");
        return Result.success(shopStatus);
    }
}
