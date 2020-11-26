package com.leyou.item.service;

import com.leyou.item.mapper.CategoryMapper;
import com.leyou.item.pojo.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 清角吹寒
 * Date: 2019-01-15
 * Time: 19:42
 */
@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    public List<Category> queryListByParent(Long pid) {
        Category category = new Category();
        category.setParentId(pid);
        return this.categoryMapper.select(category);
    }

    public List<Category> queryByBrandId(Long bid) {
        return this.categoryMapper.queryByBrandId(bid);
    }

    /**
     * 查询spu的商品分类名称,由GoodsService调用
     * @param ids
     * @return
     */
    public List<String> queryNamesByIds(List<Long> ids) {

        List<Category> list = this.categoryMapper.selectByIdList(ids);
        List<String> names = new ArrayList<>();
        for (Category category : list) {
            names.add(category.getName());
        }
        return names;
    }

    public List<Category> queryAllByCid3(Long cid3) {
        Category c3 = this.categoryMapper.selectByPrimaryKey(cid3);
        Category c2 = this.categoryMapper.selectByPrimaryKey(c3.getParentId());
        Category c1 = this.categoryMapper.selectByPrimaryKey(c2.getParentId());
        return Arrays.asList(c1,c2,c3);
    }

    public void saveCategory(Category category) {
        this.categoryMapper.insertSelective(category);
    }

    //根据商品分类id查询名称
    /*public List<String> queryNameByIds(List<Long> ids) {
        return this.categoryMapper.selectByIdList(ids).stream().map(Category::getName).collect(Collectors.toList());
        *//*List<Category> list = this.categoryMapper.selectByIdList(ids);
        List<String> names = new ArrayList<>();
        for (Category category : list) {
            names.add(category.getName());
        }
        return names;*//*
    }*/
}
