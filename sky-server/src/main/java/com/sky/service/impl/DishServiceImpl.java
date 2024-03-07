package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Ikaros
 */
@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;


    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     */
    @Transactional
    @Override
    public void saveWithFlavor(DishDTO dishDTO) {
        log.info("向菜品表和口味表同时插入数据");
        //向菜品表插入一条数据
        Dish d = new Dish();
        BeanUtils.copyProperties(dishDTO, d);
        //此处在插入时，mybatis-plus会自动帮忙设置主键id
        dishMapper.insert(d);
        log.info("dish_id: {}", d.getId());
        Long dishId = d.getId();

        //向口味表插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();

        if (flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    @Override
    public PageResult page(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        List<DishVO> list = dishMapper.pageQuery(dishPageQueryDTO);
        Page<DishVO> p = (Page<DishVO>) list;

        return new PageResult(p.getTotal(), p.getResult());
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder().id(id).status(status).build();
        dishMapper.updateById(dish);
    }

    @Override
    @Transactional //开始事务，保证原子性
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能够删除 是否是启售的菜品
        List<Dish> dishes = dishMapper.selectBatchIds(ids);
        dishes.forEach(dish -> {
            if (dish.getStatus() == StatusConstant.ENABLE){
                //当前菜品启售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }

        });

        //判断当前菜品是否被套餐关联

        LambdaQueryWrapper<SetmealDish> lqw = new LambdaQueryWrapper<>();
        lqw.in(SetmealDish::getDishId, ids);
        List<SetmealDish> setmealDishes = setmealDishMapper.selectList(lqw);
        log.info("setmealDishes: {}", setmealDishes);
        if (setmealDishes != null && setmealDishes.size() > 0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }


        //删除菜品表中的菜品
        dishMapper.deleteBatchIds(ids);

        //删除口味数据
        /*for (Long id : ids){
            LambdaQueryWrapper<DishFlavor> dishFlavorLqw = new LambdaQueryWrapper<>();
            dishFlavorLqw.eq(DishFlavor::getDishId, id);
            dishFlavorMapper.delete(dishFlavorLqw);
        }*/
        LambdaQueryWrapper<DishFlavor> dishFlavorLqw = new LambdaQueryWrapper<>();
        dishFlavorLqw.in(DishFlavor::getDishId, ids);
        dishFlavorMapper.delete(dishFlavorLqw);

    }


    @Override
    public DishVO getByIdWithFlavors(Long id) {

        DishVO dv1 = dishMapper.getByIdWithCategoryName(id);
        //创建Flavors查询包装器
        //根据id查询口味数据
        LambdaQueryWrapper<DishFlavor> lqw = new LambdaQueryWrapper<>();
        lqw.eq(DishFlavor::getDishId, id);
        List<DishFlavor> dishFlavors = dishFlavorMapper.selectList(lqw);
        dv1.setFlavors(dishFlavors);
        return dv1;
    }

    @Override
    @Transactional
    public void updateDishWithFlavors(DishDTO dishDTO) {
        //可以把原来的口味数据统一先删掉，然后再把新的口味数据添加上
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //修改菜品基本表信息
        dishMapper.updateById(dish);

        //删除原有口味表数据
        LambdaQueryWrapper<DishFlavor> lqw = new LambdaQueryWrapper<>();
        lqw.eq(DishFlavor::getDishId, dishDTO.getId());
        dishFlavorMapper.delete(lqw);

        //添加新的口味
        Long dishId = dishDTO.getId();
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 根据分类id查询菜品 管理端
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> getByCategoryId(Long categoryId) {
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Dish::getCategoryId, categoryId);
        List<Dish> dishes = dishMapper.selectList(lqw);
        return dishes;
    }

    @Override
    public List<DishVO> listWithFlavor(Dish dish) {
        List<DishVO> dishVOList = dishMapper.list(dish);
        dishVOList.forEach(dishVO -> {
            LambdaQueryWrapper<DishFlavor> lqw = new LambdaQueryWrapper<>();
            lqw.eq(DishFlavor::getDishId, dishVO.getId());
            List<DishFlavor> dishFlavors = dishFlavorMapper.selectList(lqw);
            dishVO.setFlavors(dishFlavors);
        });
        return dishVOList;
    }
}
