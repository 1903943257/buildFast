package com.jiaojiao.yuaicodemother.core;

import com.jiaojiao.yuaicodemother.ai.AiCodeGeneratorService;
import com.jiaojiao.yuaicodemother.ai.model.HtmlCodeResult;
import com.jiaojiao.yuaicodemother.ai.model.MultiFileCodeResult;
import com.jiaojiao.yuaicodemother.core.parser.CodeParseExecutor;
import com.jiaojiao.yuaicodemother.core.saver.CodeFileSaverExecutor;
import com.jiaojiao.yuaicodemother.exception.BusinessException;
import com.jiaojiao.yuaicodemother.exception.ErrorCode;
import com.jiaojiao.yuaicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI代码生成门面类，组合代码生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * 统一入口：生成并保存代码
     * @param userMessage 用户提示词
     * @param codeGenTypeEnum 代码生成类型
     * @return 代码
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if(codeGenTypeEnum == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield  CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE);
            }
            default -> {
                String errorMessage = "不支持的生成类型:" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.PARAMS_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：生成并保存代码 (流式)
     * @param userMessage 用户提示词
     * @param codeGenTypeEnum 代码生成类型
     * @return 代码
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if(codeGenTypeEnum == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(result, CodeGenTypeEnum.HTML);
            }
            case MULTI_FILE -> {
                Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield  processCodeStream(result, CodeGenTypeEnum.MULTI_FILE);
            }
            default -> {
                String errorMessage = "不支持的生成类型:" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.PARAMS_ERROR, errorMessage);
            }
        };
    }


    /**
     * 通用模式代码并保存（流式）
     * @param codeStream 代码流
     * @param codeGenType 代码生成类型
     * @return 返回流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType) {
        // 字符串拼接器，用于当流式返回所有代码后，再保存代码
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
            // 实时收集代码片段
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {
            //  流式返回完成后，保存代码
            try{
                String completeCode = codeBuilder.toString();
                //  使用执行器保存代码
                Object parsedResult = CodeParseExecutor.executeParser(completeCode, codeGenType);
                //  保存代码到文件
                File saveDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType);
                log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
            } catch (Exception e){
                log.info("保存失败{}", e.getMessage());
            }
        });
    }


}
