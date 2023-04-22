package com.itheima.reggie.controller;


import com.itheima.reggie.common.R;
import com.sun.org.apache.xpath.internal.operations.Mult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.UUID;

/**
 * 文件上传与下载
 */
@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController {

    //动态获取文件上传的位置
    @Value("${reggie.path}")
    private String basePath;
    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file){
        //file是一个临时文件，需要转存到指定位置，否则本次请求完成后会删除
        log.info(file.toString());

        //获取原始文件名
        String originalFilename = file.getOriginalFilename();//假设abc.jpg
        //动态获取文件名后缀
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));//.jap

        //使用UUID重新生成文件名，防止文件名重复造成覆盖
        String fileName = UUID.randomUUID().toString()+suffix;//fsfafa.jpg

        //创建一个目录
        File dir=new File(basePath);
        //判断当前目录是否存在
        if(!dir.exists()){
            //目录不存在，需要创建
            dir.mkdir();
        }

        try {
            //将临时文件转存到指定位置
            file.transferTo(new File(basePath+fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //返回文件名
        return R.success(fileName);
    }

    /**
     * 文件下载
     * @param name
     * @param response
     */
    @GetMapping("/download")
    public void download(String name, HttpServletResponse response){

        try {
            //输入流，通过输入流读取文件内容
            FileInputStream fileInputStream=new FileInputStream(new File(basePath+name));

            //输出流，通过输出流将文件写回给浏览器，在浏览器展示图片了
            ServletOutputStream outputStream=response.getOutputStream();

            //设置响应回去的文件
            response.setContentType("image/jpeg");

            int len=0;
            byte[] bytes=new byte[1024];
            while ((len=fileInputStream.read(bytes))!=-1){
                outputStream.write(bytes,0,len);
                //刷新
                outputStream.flush();

                //释放资源
              /*  outputStream.close();
                fileInputStream.close();*/
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
