package com.leyou.item.controller;

import com.leyou.item.pojo.Category;
import com.leyou.item.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 清角吹寒
 * Date: 2019-01-15
 * Time: 19:17
 */
@RestController
@RequestMapping(value = "category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 根据父节点查询商品类目
     * @param pid
     * @return
     */
    @GetMapping("list")
    public ResponseEntity<List<Category>> queryByParentId(
            @RequestParam(value = "pid",defaultValue = "0")Long pid
    ){
        List<Category> list = this.categoryService.queryListByParent(pid);
        if (list == null || list.size()<1) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(list);
    }

    /**
     * 新增节点
     * @param category
     * @return
     */
    @PostMapping
    public ResponseEntity<Long> saveCategory(Category category){
        try {
            Long cid = this.categoryService.saveCategory(category);
            return ResponseEntity.ok(cid);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * 修改节点
     * @param cid
     * @param name
     * @return
     */
    @PutMapping(produces = "application/x-www-form-urlencoded;charset=UTF-8")
    public ResponseEntity<Void> updateCategory(@RequestParam("cid") Long cid,@RequestParam("name") String name){
        this.categoryService.updateCategory(cid,name);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * 删除节点
     * @param cid
     * @return
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteCategory(@RequestParam("cid") Long cid){
        this.categoryService.deleteCategory(cid);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * 通过品牌id查询商品分类
     * @param bid
     * @return
     */
    @GetMapping("bid/{bid}")
    public ResponseEntity<List<Category>> queryByBrandId(@PathVariable("bid") Long bid){
        List<Category> list = this.categoryService.queryByBrandId(bid);
        if (list == null || list.size()<1) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(list);
    }

    /**
     * 根据商品分类id查询名称
     * @param ids 要查询的分类id集合
     * @return 多个名称的集合
     */
    @GetMapping("names")
    public ResponseEntity<List<String>> queryNameByIds(@RequestParam("ids") List<Long> ids){
        List<String> list = this.categoryService.queryNamesByIds(ids);
        if (list == null || list.size()<1){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(list);
    }

    /**
     * 根据3级分类id,查询1~3级的分类
     * @param id
     * @return
     */
    @GetMapping("all/level")
    public ResponseEntity<List<Category>> queryAllByCid3(@RequestParam("id") Long id){
        List<Category> list = this.categoryService.queryAllByCid3(id);
        if (list == null || list.size()<1){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(list);
    }
}
