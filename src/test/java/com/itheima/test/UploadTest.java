package com.itheima.test;

import org.junit.jupiter.api.Test;

public class UploadTest {

    @Test
    public void test1(){
        String fileName="hello.jpg";
        //动态获取后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        System.out.println(suffix);
    }
}
