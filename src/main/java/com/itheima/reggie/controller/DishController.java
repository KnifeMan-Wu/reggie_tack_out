package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {

    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private DishService dishService;
    @Autowired
    private CategoryService categoryService;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());

        dishService.saveWithFlavor(dishDto);
        return R.success("新增菜品成功");
    }

    /**
     * 菜品分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        //分页构造器
        Page<Dish> pageInfo=new Page<>(page,pageSize);
        //页面展示中没有菜品分类这一栏，所有单靠上面的构造器不能展示完全
        Page<DishDto> dishDtoPage=new Page<>();
        //添加条件构造器
        LambdaQueryWrapper<Dish> queryWrapper=new LambdaQueryWrapper<>();
        //添加过滤条件
        //当输入的name不为空时，进行模糊查询
        queryWrapper.like(name!=null,Dish::getName,name);
        //添加排序条件
        queryWrapper.orderByAsc(Dish::getUpdateTime);

        //执行分页查询
        dishService.page(pageInfo,queryWrapper);

        //对象拷贝
        //将pageInfo中除了records集合的值，全部拷贝到dishDtoPage中，因为records中数据不完整
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");

        //获取pageInfo中records的集合
        List<Dish> records = pageInfo.getRecords();
        //因为records中没有菜品分类这一属性，我们需要对records进行处理，增强这一属性
        List<DishDto> list=records.stream().map((item)->{
            //item是每一个dish对象
            DishDto dishDto=new DishDto();
            //将dish对象的值拷贝到dishDto中
            BeanUtils.copyProperties(item,dishDto);

            //获取分类id
            Long categoryId = item.getCategoryId();
            //根据id查询分类对象->需要注入category业务层接口
            Category category = categoryService.getById(categoryId);
            if(category!=null){
                //获取分类对象中的菜品分类名称->这是最重要的，我们需要的就是这个值
                String categoryName = category.getName();

                //将菜品分类名称赋值给dishDto对象
                dishDto.setCategoryName(categoryName);
            }


            return dishDto;
        }).collect(Collectors.toList());

        //设置分页构造器中的条件
        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }


    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){
        //需要查询到两张表，我们在业务层进行封装

        DishDto dishDto = dishService.getByIdWithFlavors(id);
        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());

        dishService.updateWithFlavors(dishDto);

        return R.success("修改菜品信息成功");
    }

/*
    *//**
     * 根据id删除菜品
     * @param ids
     * @return
     *//*
    @DeleteMapping
    public R<String> delete(Long ids){
        log.info("删除菜品id为：{}",ids);

        dishService.remove(ids);
        return null;
    }*/
}
