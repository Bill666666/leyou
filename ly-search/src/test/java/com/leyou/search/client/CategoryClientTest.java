package com.leyou.search.client;

import com.leyou.LySearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 清角吹寒
 * Date: 2019-02-04
 * Time: 13:27
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = LySearchService.class)
public class CategoryClientTest {
    @Autowired
    private CategoryClient categoryClient;

    @Test
    public void testCategory() {
        List<String> list = this.categoryClient.queryNameByIds(Arrays.asList(1L, 2L, 3L));
        System.out.println("list = " + list);

    }
}