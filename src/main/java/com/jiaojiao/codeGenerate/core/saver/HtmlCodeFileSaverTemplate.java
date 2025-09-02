package com.jiaojiao.codeGenerate.core.saver;

import cn.hutool.core.util.StrUtil;
import com.jiaojiao.codeGenerate.ai.model.HtmlCodeResult;
import com.jiaojiao.codeGenerate.exception.BusinessException;
import com.jiaojiao.codeGenerate.exception.ErrorCode;
import com.jiaojiao.codeGenerate.model.enums.CodeGenTypeEnum;

public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }

    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        // html代码不能为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "html代码内容不能为空");
        }
    }
}
