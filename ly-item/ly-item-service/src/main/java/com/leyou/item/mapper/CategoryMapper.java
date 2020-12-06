package com.leyou.item.mapper;

import com.leyou.item.pojo.Category;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.additional.idlist.SelectByIdListMapper;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 清角吹寒
 * Date: 2019-01-15
 * Time: 19:47
 */

public interface CategoryMapper extends Mapper<Category>, SelectByIdListMapper<Category,Long> {

    /**
     * 根据品牌id查询商品分类
     * @param bid
     * @return
     */
    @Select("SELECT * FROM tb_category WHERE id IN (SELECT category_id FROM tb_category_brand WHERE brand_id = #{bid})")
    List<Category> queryByBrandId(Long bid);

    @Options(useGeneratedKeys = true,keyProperty = "id",keyColumn = "id")
    @Insert("INSERT tb_category (name,parent_id,is_parent,sort) VALUES (#{name},#{parentId},#{isParent},#{sort})")
    Long insertCategory(Category category);
}
