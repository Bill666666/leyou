package com.leyou.upload.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 清角吹寒
 * Date: 2019-01-18
 * Time: 9:58
 */
@Service
public class UploadService {

    private static final Logger logger = LoggerFactory.getLogger(UploadService.class);

    //支持的文件类型
    private static final List<String> suffixes = Arrays.asList("image/png", "image/jpeg");

    @Autowired
    FastFileStorageClient storageClient;

    public String upload(MultipartFile file) {

        try {
            //1 图片信息校验
            //1)校验文件类型
            String type = file.getContentType();
            if (!suffixes.contains(type)) {
                logger.info("上传失败,文件类型不匹配:{}",type);
                return null;
            }
            //2)校验图片内容
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                logger.info("上传失败,文件内容不符合要求");
                return null;
            }
            //2 保存图片
            //2.1 生成保存目录
//            File dir = new File("E:/test/upload");
//            if (!dir.exists()) {
//                dir.mkdirs();
//            }
            //2.2保存图片
//            file.transferTo(new File(dir,file.getOriginalFilename()));
            //2.3拼接图片地址
//            String url = "http://image.leyou.com/upload/" + file.getOriginalFilename();

            //2 将图片上传到FastDFS
            //2.1 获取文件后缀名
            String extension = StringUtils.substringAfterLast(file.getOriginalFilename(), ".");
            // 2.2、上传
            StorePath storePath = this.storageClient.uploadFile(
                    file.getInputStream(), file.getSize(), extension, null);
            // 2.3、返回完整路径
            return "http://image.leyou.com/" + storePath.getFullPath();

        } catch (Exception e) {
            return null;
        }

    }
}
