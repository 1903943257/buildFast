package com.jiaojiao.yuaicodemother.generator;

import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;

/**
 * MyBatis Flex 代码生成器
 */
public class MyBatisCodeGenerator {

    // 要生成的表名
    private static final String[] TABLE_NAMES = {"user"};

    public static void main(String[] args) {
        // 获取数据元数据
        Dict dict = YamlUtil.loadByPath("application.yml");
        Map<String, Object> dataSourceConfig= dict.getByPath("spring.datasource");
        String url = String.valueOf(dataSourceConfig.get("url"));
        String username = String.valueOf(dataSourceConfig.get("username"));
        String password = String.valueOf(dataSourceConfig.get("password"));
        // 配置数据源
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 创建配置内容，两种风格都可以。
        GlobalConfig globalConfig = createGlobalConfig();

        // 通过 datasource 和 globalConfig 创建代码生成器
        Generator generator = new Generator(dataSource, globalConfig);

        // 生成代码
        generator.generate();
    }

    public static GlobalConfig createGlobalConfig() {
        //创建配置内容
        GlobalConfig globalConfig = new GlobalConfig();

        //设置根包,先放到临时目录里，在移动到对应目录
        globalConfig.getPackageConfig()
                .setBasePackage("com.jiaojiao.yuaicodemother.genresult");

        //设置表前缀和只生成哪些表，setGenerateTable 未配置时，生成所有表
        globalConfig.getStrategyConfig()
                .setGenerateTable(TABLE_NAMES)
                        .setLogicDeleteColumn("isDelete");

        //设置生成 entity 并启用 Lombok
        globalConfig.enableEntity()
                .setWithLombok(true)
                .setJdkVersion(21);

        //设置生成 mapper
        globalConfig.enableMapper();
        globalConfig.enableMapperXml();
        // 设置生成Service
        globalConfig.enableService();
        globalConfig.enableServiceImpl();
        // 设置生成 Controller
        globalConfig.enableController();

        // 设置生成注释，比如生成时间和作者
        globalConfig.getJavadocConfig()
                .setAuthor("jiaojiao")
                .setSince("");

        return globalConfig;
    }
}