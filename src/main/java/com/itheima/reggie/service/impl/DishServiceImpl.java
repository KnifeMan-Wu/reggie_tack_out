package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;
    /**
     * 新增菜品，同时保存对应的口味数据
     * @param dishDto
     */
    //加入事务
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品的基本信息到表dish
        this.save(dishDto);

        Long dishId = dishDto.getId();//菜品ID

        //菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors();
        //为这个集合赋值菜品id
       flavors= flavors.stream().map((item) -> {
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

        //保存菜品口味数据到菜品口味数据表dish_flavor
       dishFlavorService.saveBatch(flavors);//

    }

    /**
     * //根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    public DishDto getByIdWithFlavors(Long id) {
        //查询菜品基本信息，从dish表中查
        Dish dish = this.getById(id);

        //返回的是一个dishDto对象
        DishDto dishDto=new DishDto();
        //进行拷贝
        BeanUtils.copyProperties(dish,dishDto);

        //查询对应的菜品口味信息，从dish_flavors表中查
        LambdaQueryWrapper<DishFlavor> queryWrapper=new LambdaQueryWrapper<>();
        //添加查询条件
        queryWrapper.eq(DishFlavor::getDishId,dish.getId());
        //进行查询
        List<DishFlavor> flavors= dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(flavors);

        return dishDto;
    }

    /**
     *  更新菜品信息，同时更新口味信息
     * @param dishDto
     */
    @Transactional
    public void updateWithFlavors(DishDto dishDto) {
        //更新菜品基本信息
        this.updateById(dishDto);
        //清理当前菜品对应口味数据---dish_flavors表的delete操作
        LambdaQueryWrapper<DishFlavor> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dishDto.getId());

        dishFlavorService.remove(queryWrapper);
        //更新当前菜品对应的口味数据---dish_flavors表的insert操作
        List<DishFlavor> flavors = dishDto.getFlavors();
        //为这个集合赋值菜品id
        flavors= flavors.stream().map((item) -> {
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);

    }

  /*  *//**
     * 根据id删除分类
     * @param ids
     *//*
    public void remove(Long ids) {

    }*/
}
