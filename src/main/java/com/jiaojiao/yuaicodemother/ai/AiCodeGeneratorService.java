package com.jiaojiao.yuaicodemother.ai;

import com.jiaojiao.yuaicodemother.ai.model.HtmlCodeResult;
import com.jiaojiao.yuaicodemother.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {
    /**
     * 生成代码
     * @param userMessage 用户提示词
     * @return 代码
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * 生成多文件代码
     * @param userMessage 用户提示词
     * @return 代码
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);


    /**
     * 生成代码
     * @param userMessage 用户提示词
     * @return 代码
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    /**
     * 生成多文件代码
     * @param userMessage 用户提示词
     * @return 代码
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);

}
