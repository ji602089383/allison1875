package com.spldeolin.allison1875.handlertransformer;

import java.util.Collection;
import javax.validation.constraints.NotEmpty;
import com.spldeolin.allison1875.base.util.Configs;
import com.spldeolin.allison1875.base.util.YamlUtils;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Allison1875[handler-transformer]的配置
 *
 * @author Deolin 2020-08-25
 */
@Data
@Log4j2
public class HandlerTransformerConfig {

    @Getter
    private static final HandlerTransformerConfig instance = YamlUtils
            .toObjectAndThen("handler-transformer-config.yml", HandlerTransformerConfig.class, Configs::validate);

    /**
     * 控制层 @RequestBody类型所在包的包名
     */
    @NotEmpty
    private String reqDtoPackage;

    /**
     * 控制层 @ResponseBody业务数据部分类型所在包的包名
     */
    @NotEmpty
    private String respDtoPackage;

    /**
     * 业务层 Service接口所在包的包名
     */
    @NotEmpty
    private String servicePackage;

    /**
     * 业务 ServiceImpl类所在包的包名
     */
    @NotEmpty
    private String serviceImplPackage;

    /**
     * handler 方法上的需要生成的注解
     */
    @NotEmpty
    private Collection<@NotEmpty String> handlerAnnotations;

    /**
     * handler 方法签名的返回类型（使用%s占位符代替业务数据部分的泛型）
     */
    @NotEmpty
    private String result;

    /**
     * handler 当不需要返回业务数据时，方法签名的返回值
     */
    @NotEmpty
    private String resultVoid;

    /**
     * handler方法体的格式（使用%s占位符代替调用service的表达式）
     */
    @NotEmpty
    private String handlerBodyPattern;

    /**
     * handler不需要返回ResponseBody的场景，handler方法体的格式（使用%s占位符代替调用service的表达式）
     */
    @NotEmpty(message = "不能为空，如果不需要返回值则指定为;")
    private String handlerBodyPatternInNoResponseBodySituation;

    /**
     * controller需要确保存在的import
     */
    @NotEmpty
    private Collection<@NotEmpty String> controllerImports;

    private HandlerTransformerConfig() {
    }

}