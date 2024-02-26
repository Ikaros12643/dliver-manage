package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ikaros
 */
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;
    @Override
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        //先从DTO中拷贝出需要的setmeal数据
        //再将DTO中的setmeal_dish数据单独插入到setmeal_dish表
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);

        //处理setmeal_dish表数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(sd -> {
            sd.setSetmealId(setmeal.getId());

        });
        setmealDishMapper.insertBatch(setmealDishes);

    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        List<SetmealVO> setmeals = setmealMapper.page(setmealPageQueryDTO);
        Page<SetmealVO> p = (Page<SetmealVO>) setmeals;

        return new PageResult(p.getTotal(), p.getResult());
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //1.删除套餐表中的数据
        //启售中的套餐不能删除
        List<Setmeal> setmeals = setmealMapper.selectBatchIds(ids);
        setmeals.forEach(setmeal -> {
            if (setmeal.getStatus() == 1){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });
        setmealMapper.deleteBatchIds(ids);

        //2.根据套餐id删除套餐菜品表中的数据
        LambdaQueryWrapper<SetmealDish> lqw = new LambdaQueryWrapper<>();
        lqw.in(SetmealDish::getSetmealId, ids);
        setmealDishMapper.delete(lqw);
    }

    /**
     *
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        //首先判断套餐中是否包含未启售的菜品
        LambdaQueryWrapper<SetmealDish> lqwSd = new LambdaQueryWrapper<>();
        lqwSd.eq(SetmealDish::getSetmealId, id);
        //获取套餐菜品数据
        List<SetmealDish> setmealDishList = setmealDishMapper.selectList(lqwSd);
        //得到dishId
        List<Long> dishIdList = new ArrayList<>();
        setmealDishList.forEach(setmealDish -> {
            dishIdList.add(setmealDish.getDishId());
        });

        List<Dish> dishList = dishMapper.selectBatchIds(dishIdList);
        dishList.forEach(dish -> {
            if (dish.getStatus().equals(0)){
                //判断需启售的套餐中有没有未启售的菜品，有的话抛出异常
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED);
            }
        });
        //启售或禁售菜品
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.updateById(setmeal);
    }

    @Override
    public SetmealVO getById(Long id) {
        SetmealVO setmealVO = setmealMapper.getById(id);

        //获得套餐菜品信息并设置入返回数据
        LambdaQueryWrapper<SetmealDish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> setmealDishList = setmealDishMapper.selectList(lqw);
        setmealVO.setSetmealDishes(setmealDishList);
        return setmealVO;
    }

    @Override
    @Transactional
    public void updateSetmeal(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //更改套餐基本表
        setmealMapper.updateById(setmeal);

        //首先删除原有的套餐菜品数据
        LambdaQueryWrapper<SetmealDish> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SetmealDish::getSetmealId, setmealDTO.getId());
        setmealDishMapper.delete(lqw);
        //获取DTO中的套餐菜品数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && setmealDishes.size() > 0){
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealDTO.getId());
            });
        }
        //向套餐菜品表中插入数据
        setmealDishMapper.insertBatch(setmealDishes);
    }
}
