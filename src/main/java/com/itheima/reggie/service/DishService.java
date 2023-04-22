package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;

public interface DishService extends IService<Dish> {

    //新增菜品，同时插入菜品的口味数据，需要操作两张表dish,dish_flavor
    public void saveWithFlavor(DishDto dishDto);

    //根据id查询菜品信息和对应的口味信息
    public DishDto getByIdWithFlavors(Long id);

    //更新菜品信息，同时更新口味信息
    public void updateWithFlavors(DishDto dishDto);

   /* //根据id删除菜品
    public void remove(Long ids);*/
}
