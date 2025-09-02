package com.jiaojiao.codeGenerate.core.parser;

import com.jiaojiao.codeGenerate.exception.BusinessException;
import com.jiaojiao.codeGenerate.exception.ErrorCode;
import com.jiaojiao.codeGenerate.model.enums.CodeGenTypeEnum;

/**
 * 代码解析器执行器
 */
public class CodeParseExecutor {

    private static final HtmlCodeParser htmlCodeParser = new HtmlCodeParser();
    private static final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();

    /**
     * 执行代码解析
     * @param codeContent 代码内容
     * @param codeGenTypeEnum 代码生成类型
     * @return 解析结果（HtmlCodeResult or MultiFileCodeResult）
     */
    public static Object executeParser(String codeContent, CodeGenTypeEnum codeGenTypeEnum) {

        return switch (codeGenTypeEnum) {
            case HTML -> htmlCodeParser.parseCode(codeContent);
            case MULTI_FILE -> multiFileCodeParser.parseCode(codeContent);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的代码生成类型");
        };
    }
}
