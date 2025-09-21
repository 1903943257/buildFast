package com.jiaojiao.codeGenerate.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VueProjectBuilder {
//    /**
//     * 异步构建项目（虚拟线程，不阻塞主流程）
//     * @param projectPath 项目路径
//     */
//    public void buildProjectAsync(String projectPath) {
//        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis()).start(() -> {
//            try{
//                buildProject(projectPath);
//            } catch (Exception e) {
//                log.error("异步构建Vue项目时发生异常：{}", e.getMessage(), e);
//            }
//        });
//    }

    /**
     * 构建 Vue 项目
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            return false;
        }
        // 检查 package.json 是否存在
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            return false;
        }
        log.info("开始构建 Vue 项目: {}", projectPath);
        // 执行 npm install
        if (!executeNpmInstall(projectDir)) {
            log.error("npm install 执行失败");
            return false;
        }
        // 执行 npm run build
        if (!executeNpmBuild(projectDir)) {
            log.error("npm run build 执行失败");
            return false;
        }
        // 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            return false;
        }
        log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
        return true;
    }


//    /**
//     * 执行 npm install 命令
//     */
//    private boolean executeNpmInstall(File projectDir) {
//        log.info("执行 npm install...");
//        String command = String.format("%s install", buildCommand("npm"));
//        return executeCommand(projectDir, command, 300); // 5分钟超时
//    }

    /**
     * 执行加强版 npm install 命令
     */
    private boolean executeNpmInstall(File projectDir) {
        log.info("执行 npm install...（使用国内源加速）");
        // 1. 添加淘宝国内源，解决国外源下载慢的问题
        String npmCommand = buildCommand("npm");
        String command = String.format("%s install --registry=https://registry.npmmirror.com", npmCommand);
        // 2. 延长超时时间到10分钟（600秒），应对依赖较多的场景
        return executeCommand(projectDir, command, 300);

    }

    /**
     * 执行增强的 npm run build 命令
     */
//    private boolean executeNpmBuild(File projectDir) {
//        log.info("执行 npm run build...");
//        String command = String.format("%s run build", buildCommand("npm"));
//
//        return executeCommand(projectDir, command, 180); // 3分钟超时
//    }
    private boolean executeNpmBuild(File projectDir) {
        log.info("执行 npm run build...");
        String npmCommand = buildCommand("npm");
        // 可选：添加国内源，避免构建时下载依赖超时
        String command = String.format("%s run build --registry=https://registry.npmmirror.com", npmCommand);
        return executeCommand(projectDir, command, 180);
    }

    /**
     * 系统检测
     * @return 是否为Windows系统
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }



    /**
     * 执行命令
     *
     * @param workingDir     工作目录
     * @param command        命令字符串
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否执行成功
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            Process process = RuntimeUtil.exec(
                    null,
                    workingDir,
                    command.split("\\s+") // 命令分割为数组
            );
            // 等待进程完成，设置超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return true;
            } else {
                log.error("命令执行失败，退出码: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return false;
        }
    }

}
