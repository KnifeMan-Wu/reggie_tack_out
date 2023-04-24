package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;
    /**
     * 新增套餐 ，同时需要保存套餐和菜品的关联关系
     * @param setmealDto
     */
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        //保存套餐的基本信息，操作Setmeal表，执行insert操作
        this.save(setmealDto);

        //获取关联关系集合->setmealId没有值，需要进行处理
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes.stream().map((item)->{
            item.setSetmealId(setmealDto.getId());
            return setmealDishes;
        }).collect(Collectors.toList());

        //保存套餐和菜品的关联信息，操作表SetmealDish执行insert操作
        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * 删除套餐，删除套餐的同时删除菜品的关联关系
     * @param ids
     */
    @Transactional
    public void deleteWithDish(List<Long> ids) {
        //先查询套餐转态，确定是否可以删除
        LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();
        //查询id在ids数组中的
        queryWrapper.in(Setmeal::getId,ids);
        //查询售卖状态为1的->在售时不能删除
        queryWrapper.eq(Setmeal::getStatus,1);
        int count = this.count(queryWrapper);//查询这个套餐中售卖状态是否有为在售的

        if(count>0){
            //如果不可以删除，抛出一个业务异常
            throw new CustomException("套餐正在售卖中，不能删除");
        }

        //如果可以删除，先删除setmeal表
        this.removeByIds(ids);


        //注意->这里关系表中主键值不是我们前端传来的id值
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId,ids);
        //再删除关系表setmeal_dish表
        setmealDishService.remove(lambdaQueryWrapper);

    }

    /**
     * 根据id查询对应的套餐信息，和套餐关联菜品信息
     * @param id
     */
    public SetmealDto getByIdWithDish(Long id) {
        //查询套餐基本信息,从setmeal表中查
        Setmeal setmeal = this.getById(id);

        //返回的是一个dto对象
        SetmealDto setmealDto=new SetmealDto();
        //进行拷贝
        BeanUtils.copyProperties(setmeal,setmealDto);

        //查询套餐关联菜品信息，从setmeal_dish表中查
        LambdaQueryWrapper<SetmealDish> queryWrapper=new LambdaQueryWrapper<>();
        //添加查询条件
        queryWrapper.eq(SetmealDish::getSetmealId,setmeal.getId());
        //执行查询
        List<SetmealDish> list = setmealDishService.list(queryWrapper);

        setmealDto.setSetmealDishes(list);

        return setmealDto;
    }

    /**
     * 更新套餐信息，同时更新套餐关联菜品信息
     * @param setmealDto
     */
    public void updateWithDish(SetmealDto setmealDto) {
        //先更新表setmeal
        this.updateById(setmealDto);
        //先清理当前套餐关联菜品表的信息->setmeal_dish表的delete操作
        LambdaQueryWrapper<SetmealDish> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,setmealDto.getId());

        setmealDishService.remove(queryWrapper);
        //更新当前套餐关联的菜品表数据->setmeal_dish表的insert操作
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        //为当前集合赋值套餐id
       setmealDishes= setmealDishes.stream().map((item)->{
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

       setmealDishService.saveBatch(setmealDishes);

    }


}
