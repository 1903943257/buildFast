package com.jiaojiao.codeGenerate.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * HTML 代码结果
 */
@Description("HTML 代码结果")
@Data
public class HtmlCodeResult {

    /**
     * HTML 代码
     */
    @Description("HTML 代码")
    private String htmlCode;

    /**
     * 描述
     */
    @Description("描述")
    private String description;
}
