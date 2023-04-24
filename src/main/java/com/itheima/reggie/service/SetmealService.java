package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface SetmealService extends IService<Setmeal> {

    /**
     * 新增套餐 ，同时需要保存套餐和菜品的关联关系
     * @param setmealDto
     */
    public void saveWithDish(SetmealDto setmealDto);

    /**
     * 删除套餐，删除套餐的同时删除菜品的关联关系
     * @param ids
     */
    public void deleteWithDish(List<Long> ids);

    /**
     * 根据id查询对应的套餐信息，和套餐关联菜品信息
     * @param id
     */
    public SetmealDto getByIdWithDish(Long id);

    /**
     * 更新套餐信息，同时更新套餐关联菜品信息
     */
    @Transactional
    public void updateWithDish(SetmealDto setmealDto);
}
