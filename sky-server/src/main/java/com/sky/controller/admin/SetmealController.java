package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Ikaros
 */
@RestController
@Slf4j
@RequestMapping("/admin/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;
    @PostMapping
    public Result save(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐: {}", setmealDTO);
        setmealService.save(setmealDTO);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("套餐分页查询: {}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    public Result deleteBatch(@RequestParam List<Long> ids){
        log.info("批量删除套餐: {}", ids);
        setmealService.deleteBatch(ids);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    public Result startOrStop(@PathVariable Integer status, Long id){
        log.info("启售或停售套餐: {}", status);
        setmealService.startOrStop(status, id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("根据id查询套餐数据: {}", id);
        SetmealVO setmealVO = setmealService.getById(id);
        return Result.success(setmealVO);
    }

    @PutMapping
    public Result updateSetmeal(@RequestBody SetmealDTO setmealDTO){
        log.info("更新套餐表以及套餐-菜品表: {}", setmealDTO);
        setmealService.updateSetmeal(setmealDTO);
        return Result.success();
    }
}
