package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    List<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 返回带categoryName的dishVo
     * @param id
     * @return
     */
    DishVO getByIdWithCategoryName(Long id);

    @Select("select d.*, c.name as categoryName from dish as d " +
            "left outer join category as c " +
            "on d.category_id = c.id " +
            "where d.category_id=#{categoryId} and d.status=#{status}")
    List<DishVO> list(Dish dish);

    Integer countByMap(Map map);
}
