package com.spldeolin.allison1875.docanalyzer.processor;

import java.util.Collection;
import java.util.Set;
import javax.validation.ConstraintViolation;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.collect.Lists;
import com.spldeolin.allison1875.base.ancestor.Allison1875MainProcessor;
import com.spldeolin.allison1875.base.ast.AstForest;
import com.spldeolin.allison1875.base.ast.AstForestContext;
import com.spldeolin.allison1875.base.util.ValidateUtils;
import com.spldeolin.allison1875.base.util.ast.Annotations;
import com.spldeolin.allison1875.base.util.ast.Authors;
import com.spldeolin.allison1875.base.util.ast.JavadocDescriptions;
import com.spldeolin.allison1875.base.util.ast.MethodQualifiers;
import com.spldeolin.allison1875.docanalyzer.DocAnalyzerConfig;
import com.spldeolin.allison1875.docanalyzer.builder.EndpointDtoBuilder;
import com.spldeolin.allison1875.docanalyzer.dto.ControllerFullDto;
import com.spldeolin.allison1875.docanalyzer.dto.EndpointDto;
import com.spldeolin.allison1875.docanalyzer.dto.HandlerFullDto;
import com.spldeolin.allison1875.docanalyzer.handle.AnalyzeCustomValidationHandle;
import com.spldeolin.allison1875.docanalyzer.handle.AnalyzeEnumConstantHandle;
import com.spldeolin.allison1875.docanalyzer.handle.MoreJpdvAnalysisHandle;
import com.spldeolin.allison1875.docanalyzer.handle.ObtainConcernedResponseBodyHandle;
import com.spldeolin.allison1875.docanalyzer.handle.SpecificFieldDescriptionsHandle;
import com.spldeolin.allison1875.docanalyzer.handle.impl.DefaultAnalyzeCustomValidationHandle;
import com.spldeolin.allison1875.docanalyzer.handle.impl.DefaultAnalyzeEnumConstantHandle;
import com.spldeolin.allison1875.docanalyzer.handle.impl.DefaultMoreJpdvAnalysisHandle;
import com.spldeolin.allison1875.docanalyzer.handle.impl.DefaultObtainConcernedResponseBodyHandle;
import com.spldeolin.allison1875.docanalyzer.handle.impl.DefaultSpecificFieldDescriptionsHandle;
import lombok.extern.log4j.Log4j2;

/**
 * doc-analyzer的主流程
 *
 * @author Deolin 2020-06-10
 */
@Log4j2
public class DocAnalyzer implements Allison1875MainProcessor<DocAnalyzerConfig, DocAnalyzer> {

    protected ObtainConcernedResponseBodyHandle obtainConcernedResponseBodyHandle =
            new DefaultObtainConcernedResponseBodyHandle();

    protected AnalyzeCustomValidationHandle analyzeCustomValidationHandle = new DefaultAnalyzeCustomValidationHandle();

    protected SpecificFieldDescriptionsHandle specificFieldDescriptionsHandle =
            new DefaultSpecificFieldDescriptionsHandle();

    protected AnalyzeEnumConstantHandle analyzeEnumConstantHandle = new DefaultAnalyzeEnumConstantHandle();

    protected MoreJpdvAnalysisHandle moreJpdvAnalysisHandle = new DefaultMoreJpdvAnalysisHandle();

    ListControllersProc listControllersProc = new ListControllersProc();

    ListHandlersProc listHandlersProc = new ListHandlersProc();

    public static final ThreadLocal<DocAnalyzerConfig> CONFIG = ThreadLocal.withInitial(DocAnalyzerConfig::new);

    @Override
    public DocAnalyzer config(DocAnalyzerConfig config) {
        Set<ConstraintViolation<DocAnalyzerConfig>> violations = ValidateUtils.validate(config);
        if (violations.size() > 0) {
            log.warn("配置项校验未通过，请检查后重新运行");
            for (ConstraintViolation<DocAnalyzerConfig> violation : violations) {
                log.warn(violation.getRootBeanClass().getSimpleName() + "." + violation.getPropertyPath() + " "
                        + violation.getMessage());
            }
            System.exit(-9);
        }
        CONFIG.set(config);
        return this;
    }

    @Override
    public void process(AstForest astForest) {
        // 重新生成astForest（将解析范围扩大到所有用户配置的项目路径）
        astForest = new AstForest(astForest.getAnyClassFromHost(), CONFIG.get().getDependencyProjectPaths());
        AstForestContext.setCurrent(astForest);

        // 首次遍历并解析astForest，然后构建jsg对象，jsg对象为后续生成JsonSchema所需，构建完毕后重置astForest游标
        JsgBuildProc jsgBuildProc = new JsgBuildProc(analyzeCustomValidationHandle,
                specificFieldDescriptionsHandle.provideSpecificFieldDescriptions(), analyzeEnumConstantHandle,
                moreJpdvAnalysisHandle);
        JsonSchemaGenerator jsg = jsgBuildProc.analyzeAstAndBuildJsg(astForest);

        // 收集endpoint
        Collection<EndpointDto> endpoints = Lists.newArrayList();

        // 遍历controller
        Collection<ControllerFullDto> controllers = listControllersProc.process(astForest);
        for (ControllerFullDto controller : controllers) {

            // 处理@RequestMapping（controller的RequestMapping）
            RequestMappingProc requestMappingProcessor = new RequestMappingProc(controller.getReflection());

            // 遍历handler
            Collection<HandlerFullDto> handlers = listHandlersProc.process(controller);
            for (HandlerFullDto handler : handlers) {

                // 收集handler的描述、是否过时、作者、源码位置 等基本信息
                EndpointDtoBuilder builder = new EndpointDtoBuilder();
                builder.cat(handler.getCat());
                builder.handlerSimpleName(controller.getCoid().getName() + "_" + handler.getMd().getName());
                builder.descriptionLines(JavadocDescriptions.getAsLines(handler.getMd()));
                builder.isDeprecated(isDeprecated(controller.getCoid(), handler.getMd()));
                builder.author(Authors.getAuthor(handler.getMd()));
                builder.sourceCode(MethodQualifiers.getTypeQualifierWithMethodName(handler.getMd()));

                // 处理@RequestMapping（handler的RequestMapping）
                requestMappingProcessor.analyze(handler.getReflection());
                builder.combinedUrls(requestMappingProcessor.getCombinedUrls());
                builder.combinedVerbs(requestMappingProcessor.getCombinedVerbs());

                // 分析Request Body
                RequestBodyProc requestBodyProc = new RequestBodyProc();
                builder.requestBodyJsonSchema(requestBodyProc.analyze(jsg, handler.getMd()));

                // 分析Response Body
                ResponseBodyProc responseBodyProc = new ResponseBodyProc(obtainConcernedResponseBodyHandle);
                builder.responseBodyJsonSchema(responseBodyProc.analyze(jsg, controller.getCoid(), handler.getMd()));

                // 构建EndpointDto
                endpoints.addAll(builder.build());
            }
        }

        // 同步到YApi
        new YApiSyncProc(moreJpdvAnalysisHandle, endpoints).process();

        log.info(endpoints.size());
    }

    private boolean isDeprecated(ClassOrInterfaceDeclaration controller, MethodDeclaration handler) {
        return Annotations.isAnnotationPresent(handler, Deprecated.class) || Annotations
                .isAnnotationPresent(controller, Deprecated.class);
    }

    public DocAnalyzer obtainConcernedResponseBodyHandle(
            ObtainConcernedResponseBodyHandle obtainConcernedResponseBodyHandle) {
        this.obtainConcernedResponseBodyHandle = obtainConcernedResponseBodyHandle;
        return this;
    }

    public DocAnalyzer analyzeCustomValidationHandle(AnalyzeCustomValidationHandle analyzeCustomValidationHandle) {
        this.analyzeCustomValidationHandle = analyzeCustomValidationHandle;
        return this;
    }

    public DocAnalyzer specificFieldDescriptionsHandle(
            SpecificFieldDescriptionsHandle specificFieldDescriptionsHandle) {
        this.specificFieldDescriptionsHandle = specificFieldDescriptionsHandle;
        return this;
    }

    public DocAnalyzer analyzeEnumConstantHandle(AnalyzeEnumConstantHandle analyzeEnumConstantHandle) {
        this.analyzeEnumConstantHandle = analyzeEnumConstantHandle;
        return this;
    }

}
