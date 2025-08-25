package com.jiaojiao.yuaicodemother.service;


/**
 * 截图服务
 */
public interface ScreenshotService {
    /**
     * 通用的截图服务
     * @param webUrl 网址
     * @return boolean
     */
    String generateAndUploadScreenshot(String webUrl);

}
