package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

/**
 * @author Ikaros
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    Integer countByMap(Map map);
}
