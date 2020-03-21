package com.leyou.page.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 清角吹寒
 * Date: 2019-02-16
 * Time: 15:41
 */
public class ThreadUtils {
    private static final ExecutorService es = Executors.newFixedThreadPool(10);
    public static void execute(Runnable runnable) {
        es.submit(runnable);
    }
}
