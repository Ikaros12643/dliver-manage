package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;


/**
 * @author Ikaros
 */
@RestController
@Slf4j
@RequestMapping("/admin/common")
public class CommonController {
    @Autowired
    private AliOssUtil aliOssUtil;
    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) throws Exception{
        log.info("文件上传：{}", file);
        try{
            //获取初始文件名
            String originName = file.getOriginalFilename();
            log.info("originName: {}", originName);
            //将UUID随机文件名与初始文件名·的扩展名进行拼接
            String objectName = UUID.randomUUID().toString() + originName.substring(originName.lastIndexOf("."));
            //调用工具类的upload方法
            String url = aliOssUtil.upload(file.getBytes(), objectName);
            return Result.success(url);
        }catch (Exception e){
            log.error("文件上传失败");
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
