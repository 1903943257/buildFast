package com.jiaojiao.codeGenerate.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.jiaojiao.codeGenerate.ai.model.HtmlCodeResult;
import com.jiaojiao.codeGenerate.ai.model.MultiFileCodeResult;
import com.jiaojiao.codeGenerate.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 代码文件保存器
 */
@Deprecated
public class CodeFileSaver {
    /**
     * 代码文件保存根目录
     */
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";
    /**
     * 保存html代码
     * @param htmlCodeResult html代码结果
     * @return 代码文件目录
     */
    public static File saveHtmlCodeResult(HtmlCodeResult htmlCodeResult) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        writeToFile(baseDirPath, "index.html", htmlCodeResult.getHtmlCode());;
        return new File(baseDirPath);

    }

    /**
     * 保存多文件代码
     * @param multiFileCodeResult
     * @return
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult multiFileCodeResult) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        writeToFile(baseDirPath, "index.html", multiFileCodeResult.getHtmlCode());
        writeToFile(baseDirPath, "style.css", multiFileCodeResult.getCssCode());
        writeToFile(baseDirPath, "script.js", multiFileCodeResult.getJsCode());
        return new File(baseDirPath);
    }

    /**
     * 构建文件的唯一路径（tmp/code_output/bizType_雪花ID）
     * @param bizType 代码生成类型
     * @return 文件路径
     */
    private static String buildUniqueDir(String bizType) {
        String uniqueDirName = StrUtil.format("{}_{}",  bizType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 保存单个文件
     * @param dirPath 目录路径
     * @param filename 文件名
     * @param content 文件内容
     */
    private static void writeToFile(String dirPath, String filename, String content) {
        // 构建文件路径
        String filePath = dirPath + File.separator + filename;
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }
}
