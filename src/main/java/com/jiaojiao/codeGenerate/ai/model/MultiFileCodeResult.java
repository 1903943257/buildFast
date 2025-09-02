package com.jiaojiao.codeGenerate.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * 多文件代码结果
 */
@Description("多文件代码结果")
@Data
public class MultiFileCodeResult {

    /**
     * HTML 代码
     */
    @Description("HTML 代码")
    private String htmlCode;

    /**
     * CSS 代码
     */
    @Description("CSS 代码")
    private String cssCode;

    /**
     * JS 代码
     */
    @Description("JS 代码")
    private String jsCode;

    /**
     * 描述
     */
    @Description("描述")
    private String description;
}
