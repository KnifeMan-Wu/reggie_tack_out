package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Update;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());

        dishService.saveWithFlavor(dishDto);
        /* //清理Redis中所有菜品缓存数据
        Set keys=redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);*/

        //清理Redis中某一分类的缓存数据
        String key="dish_"+dishDto.getCategoryId()+"_1";
        redisTemplate.delete(key);
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

       /* //清理Redis中所有菜品缓存数据
        Set keys=redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);*/

        //清理Redis中某一分类的缓存数据
        String key="dish_"+dishDto.getCategoryId()+"_1";
        redisTemplate.delete(key);

        return R.success("修改菜品信息成功");
    }

    /**
     * 根据id删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(String ids){
        //前端页面将删除和批量删除同用一个方法，两个删除的ids肯定是不同的，单个删除是Long ids，批量删除是checkList的字符串
        //所有我们用string来接收
        //处理String，将前端传过来的字符串分割成id数组
        String[] split = ids.split(",");

        //每个id还是字符串，转为Long
        List<Long> idList = Arrays.stream(split).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        //执行批量删除
        dishService.removeByIds(idList);
        log.info("删除菜品ids为：{}",ids);


        return R.success("删除成功");
    }

    /**
     * 批量起售与停售
     * @param ids
     * @param status
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> updateStatus(Long[] ids,@PathVariable int status){
        //将数组转为集合
     //   String[] split = ids.split(",");
        List<Long> idList = Arrays.asList(ids);
        //每个id还是字符串，转为Long
      //  List<Long> idList = Arrays.stream(split).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        //创建更新的条件构造器
        LambdaUpdateWrapper<Dish> luw=new LambdaUpdateWrapper<>();
        //设置修改状态和条件
        luw.set(Dish::getStatus,status).in(Dish::getId,idList);
        //执行更新操作
        dishService.update(luw);
        log.info("菜品状态更新为：{status}",status);

        return R.success("操作成功");
    }


    /**
     * 根据分类id查询菜品分类的菜品
     * @param dish
     * @return
     */
   /* @GetMapping("/list")
    public R<List<Dish>> list(Dish dish){
        //构造条件查询器
        LambdaQueryWrapper<Dish> queryWrapper=new LambdaQueryWrapper<>();
        //添加查询条件->根据分类id查询菜品
        queryWrapper.eq(dish.getCategoryId()!=null,Dish::getCategoryId,dish.getCategoryId());
        //添加查询条件->起售才查，停售不查
        queryWrapper.eq(Dish::getStatus,1);

        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        //执行查询
        List<Dish> list = dishService.list(queryWrapper);



        return R.success(list);
    }*/

    /**
     * 根据分类id查询菜品分类的菜品
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        List<DishDto> dishDtoList=null;

        //定义redis的key
        String key="dish_"+dish.getCategoryId()+"_"+dish.getStatus();//dish_1397844391040167938_1
        //先从Redis中获取菜品数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        //如果有则直接返回，无需查询数据库
        if(dishDtoList!=null){
            return R.success(dishDtoList);
        }


        //构造条件查询器
        LambdaQueryWrapper<Dish> queryWrapper=new LambdaQueryWrapper<>();
        //添加查询条件->根据分类id查询菜品
        queryWrapper.eq(dish.getCategoryId()!=null,Dish::getCategoryId,dish.getCategoryId());
        //添加查询条件->起售才查，停售不查
        queryWrapper.eq(Dish::getStatus,1);

        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        //执行查询
        List<Dish> list = dishService.list(queryWrapper);

        dishDtoList=list.stream().map((item)->{
            DishDto dishDto=new DishDto();
            //对象拷贝
            BeanUtils.copyProperties(item,dishDto);

          /*  //获取分类id
            Long categoryId = item.getCategoryId();
            //根据分类id查询分类对象
            Category category = categoryService.getById(categoryId);
            if(category!=null){
                //获取分类对象中的菜品分类名称
                String categoryName = category.getName();
                //将菜品分类名称赋值给dishDto对象
                dishDto.setCategoryName(categoryName);
            }*/
            //获取菜品id
            Long dishId = item.getId();
            //根据菜品id查询该菜品对应的口味数据表
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper=new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DishFlavor::getDishId,dishId);

            List<DishFlavor> dishFlavors = dishFlavorService.list(lambdaQueryWrapper);
            //设置菜品口味数据
            dishDto.setFlavors(dishFlavors);
            return dishDto;
        }).collect(Collectors.toList());

        //如果没有则查询数据库，并将查询到的菜品数据放入Redis
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }
}
