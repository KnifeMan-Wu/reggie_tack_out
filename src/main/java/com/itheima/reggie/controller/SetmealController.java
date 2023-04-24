package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */
@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private CategoryService categoryService;

    /**
     * 新增套餐
     * @param setmealDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto){

        log.info("新增套餐信息为: {}",setmealDto);

        setmealService.saveWithDish(setmealDto);

        return R.success("新增套餐成功");
    }

    /**
     * 根据name进行套餐分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        //构造分页查询器
        Page<Setmeal> pageInfo=new Page<>(page,pageSize);
        //上个分页查询器没有菜品分类这个属性，所有需要构造dto的分页查询器
        Page<SetmealDto> dtoPage=new Page<>();

        //构造条件查询器
        LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();
        //添加查询条件->根据name进行模糊查询
        queryWrapper.like(name!=null,Setmeal::getName,name);
        //添加排序条件->根据修改时间降序排
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        //执行套餐分页查询
        setmealService.page(pageInfo,queryWrapper);

        //对象拷贝->将原有分页查询器除封装的数据之外全部拷贝给dtoPage
        BeanUtils.copyProperties(pageInfo,dtoPage,"records");
        //获取records集合
        List<Setmeal> records = pageInfo.getRecords();
        //records没有套餐分类这一属性，我们需要进行增强处理
        List<SetmealDto> list=records.stream().map((item)->{
            SetmealDto setmealDto=new SetmealDto();
            //对象拷贝
            BeanUtils.copyProperties(item,setmealDto);

            //获取套餐分类id
            Long categoryId = item.getCategoryId();
            //根据id进行查询
            Category category = categoryService.getById(categoryId);
            if(category!=null){
                //获取分类对象中的分类名称->这是目的
                String categoryName = category.getName();

                //将套餐名称赋值给dto对象
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());
        //设置新分页构造器的条件
        dtoPage.setRecords(list);

        return R.success(dtoPage);
    }

    /**
     * 删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){

        log.info("ids:{}",ids);

        setmealService.deleteWithDish(ids);
        return R.success("删除套餐成功");
    }

    /**
     * 根据id查询对应的套餐信息，和套餐关联菜品信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<SetmealDto> get(@PathVariable Long id){
        log.info("id:{id}",id);
        //需要查询到两张表，我们在业务层进行封装
        SetmealDto setmealDto = setmealService.getByIdWithDish(id);

        return R.success(setmealDto);
    }

    /**
     * 修改套餐信息
     * @param setmealDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto){
        log.info(setmealDto.toString());

        setmealService.updateWithDish(setmealDto);
        return R.success("修改套餐信息成功");
    }

    @PostMapping("/status/{status}")
    public R<String> updateStatus(Long[] ids,@PathVariable int status){
        //将数组转为集合
        List<Long> idList = Arrays.asList(ids);
        //创建更新状态的条件构造器
        LambdaUpdateWrapper<Setmeal> luw=new LambdaUpdateWrapper<>();
        //设置修改状态和条件
        luw.set(Setmeal::getStatus,status).in(Setmeal::getId,idList);
        //执行更新操作
        setmealService.update(luw);
        log.info("status:{status}",status);

        return R.success("操作成功");
    }
}
