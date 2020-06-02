package com.spldeolin.allison1875.da.approved;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema.Items;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ValueTypeSchema;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.spldeolin.allison1875.base.collection.ast.AstForest;
import com.spldeolin.allison1875.base.constant.QualifierConstants;
import com.spldeolin.allison1875.base.exception.QualifierAbsentException;
import com.spldeolin.allison1875.base.util.JsonSchemaUtils;
import com.spldeolin.allison1875.base.util.JsonUtils;
import com.spldeolin.allison1875.base.util.LoadClassUtils;
import com.spldeolin.allison1875.base.util.ast.Authors;
import com.spldeolin.allison1875.base.util.ast.Javadocs;
import com.spldeolin.allison1875.base.util.ast.Locations;
import com.spldeolin.allison1875.base.util.ast.MethodQualifiers;
import com.spldeolin.allison1875.base.util.exception.JsonsException;
import com.spldeolin.allison1875.da.approved.builder.EndpointDtoBuilder;
import com.spldeolin.allison1875.da.approved.dto.CodeAndDescriptionDto;
import com.spldeolin.allison1875.da.approved.dto.EndpointDto;
import com.spldeolin.allison1875.da.approved.dto.JsonPropertyDescriptionValueDto;
import com.spldeolin.allison1875.da.approved.dto.PropertiesContainerDto;
import com.spldeolin.allison1875.da.approved.dto.PropertyDto;
import com.spldeolin.allison1875.da.approved.dto.PropertyValidatorDto;
import com.spldeolin.allison1875.da.approved.enums.BodySituation;
import com.spldeolin.allison1875.da.approved.enums.JsonFormatEnum;
import com.spldeolin.allison1875.da.approved.enums.JsonTypeEnum;
import com.spldeolin.allison1875.da.approved.jackson.MappingJacksonAnnotationIntrospector;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

/**
 * @author Deolin 2020-06-01
 */
@Log4j2
public class DocAnanlyzerBoot {

    public static void main(String[] args) {
        new DocAnanlyzerBoot().process();
    }

    private static final AstForest astForest = AstForest.getInstance();

    private static final PathMatcher pathMatcher = new AntPathMatcher();

    public void process() {
        Table<String, String, String> enumDescriptions = HashBasedTable.create();
        Table<String, String, String> propertyDescriptions = HashBasedTable.create();
        for (CompilationUnit cu : astForest) {
            for (TypeDeclaration<?> td : cu.findAll(TypeDeclaration.class)) {
                td.ifEnumDeclaration(ed -> collectEnumDescription(ed, enumDescriptions));
                td.ifClassOrInterfaceDeclaration(coid -> collectPropertiesAnnoInfo(coid, propertyDescriptions));
            }
        }
        JsonSchemaGenerator jsg = buildJsg(enumDescriptions, propertyDescriptions);

        astForest.reset();
        for (CompilationUnit cu : astForest) {
            for (ClassOrInterfaceDeclaration controller : cu
                    .findAll(ClassOrInterfaceDeclaration.class, this::isController)) {
                Class<?> controllerClass;
                try {
                    controllerClass = tryReflectController(controller);
                } catch (Exception e) {
                    continue;
                }
                EndpointDtoBuilder builder = new EndpointDtoBuilder();

                RequestMapping controllerRequestMapping = findRequestMappingAnnoOrElseNull(controllerClass);
                String[] cPaths = findValueFromAnno(controllerRequestMapping);
                RequestMethod[] cVerbs = findVerbFromAnno(controllerRequestMapping);

                Map<String, MethodDeclaration> methods = Maps.newHashMap();
                for (MethodDeclaration method : controller.findAll(MethodDeclaration.class)) {
                    methods.put(MethodQualifiers.getShortestQualifiedSignature(method), method);
                }

                for (Method reflectionMethod : controllerClass.getDeclaredMethods()) {
                    if (isNotHandler(reflectionMethod)) {
                        continue;
                    }

                    RequestMapping methodRequestMapping = findRequestMappingAnnoOrElseNull(reflectionMethod);
                    String[] mPaths = methodRequestMapping.value();
                    RequestMethod[] mVerbs = methodRequestMapping.method();

                    builder.combinedUrls(combineUrl(cPaths, mPaths));
                    builder.combinedVerbs(combineVerb(cVerbs, mVerbs));

                    MethodDeclaration handler = methods
                            .get(MethodQualifiers.getShortestQualifiedSignature(reflectionMethod));
                    if (handler == null) {
                        // 可能是源码删除了某个handler但未编译，所以reflectionMethod还存在，但MethodDeclaration已经不存在了，忽略即可
                        continue;
                    }

                    builder.description(Javadocs.extractEveryLine(handler, "\n"));
                    builder.version("");
                    builder.isDeprecated(isDeprecated(controller, handler));
                    builder.author(Authors.getAuthor(handler));
                    builder.sourceCode(Locations.getRelativePathWithLineNo(handler));

                    BodySituation requestBodySituation;
                    try {
                        ResolvedType requestBody = findRequestBody(handler);
                        if (requestBody != null) {
                            String requestBodyDescribe = requestBody.describe();
                            JsonSchema jsonSchema = JsonSchemaUtils
                                    .generateSchema(requestBodyDescribe, astForest.getCurrentClassLoader(), jsg);

                            if (jsonSchema.isObjectSchema()) {
                                requestBodySituation = BodySituation.NEITHER;
                                PropertiesContainerDto propContainer = anaylzeObjectSchema(requestBodyDescribe,
                                        jsonSchema.asObjectSchema());
                                builder.flatRequestProperties(propContainer.getFlatProperties());
                            } else {
                                requestBodySituation = BodySituation.CHAOS;
                                builder.requestBodyJsonSchema(JsonUtils.toJson(jsonSchema));
                            }
                        } else {
                            requestBodySituation = BodySituation.NONE;
                        }
                    } catch (Exception e) {
                        log.error(e);
                        requestBodySituation = BodySituation.FAIL;
                    }
                    builder.requestBodySituation(requestBodySituation);

                    BodySituation responseBodySituation;
                    try {
                        ResolvedType responseBody = findResponseBody(controller, handler);
                        if (responseBody != null) {
                            String responseDescribe = responseBody.describe();
                            JsonSchema jsonSchema = JsonSchemaUtils
                                    .generateSchema(responseDescribe, astForest.getCurrentClassLoader(), jsg);

                            if (jsonSchema.isObjectSchema()) {
                                responseBodySituation = BodySituation.NEITHER;
                                PropertiesContainerDto propContainer = anaylzeObjectSchema(responseDescribe,
                                        jsonSchema.asObjectSchema());
                                clearAllValidatorAndNullableFlag(propContainer);
                                builder.flatResponseProperties(propContainer.getFlatProperties());
                            } else {
                                responseBodySituation = BodySituation.CHAOS;
                                builder.responseBodyJsonSchema(JsonUtils.toJson(jsonSchema));
                            }
                        } else {
                            responseBodySituation = BodySituation.NONE;
                        }
                    } catch (Exception any) {
                        log.error("", any);
                        responseBodySituation = BodySituation.FAIL;
                    }
                    builder.responseBodySituation(responseBodySituation);

                    EndpointDto endpoint = builder.build();
                    log.info("builder={}", endpoint);
                }
            }
        }
    }

    private void clearAllValidatorAndNullableFlag(PropertiesContainerDto propContainer) {
        for (PropertyDto prop : propContainer.getFlatProperties()) {
            prop.setNullable(null);
            prop.setValidators(null);
        }
    }

    private PropertiesContainerDto anaylzeObjectSchema(String requestBodyDescribe, ObjectSchema objectSchema) {
        PropertyDto tempParent = new PropertyDto();
        calcObjectTypeWithRecur(tempParent, objectSchema, false);
        List<PropertyDto> dendriformProperties = getDendriformPropertiesFromTemp(tempParent);
        return new PropertiesContainerDto(requestBodyDescribe, dendriformProperties);
    }

    private List<PropertyDto> getDendriformPropertiesFromTemp(PropertyDto tempParent) {
        return tempParent.getChildren().stream().map(child -> child.setParent(null)).collect(Collectors.toList());
    }

    private JsonTypeEnum calcObjectTypeWithRecur(PropertyDto parent, ObjectSchema parentSchema, boolean isInArray) {
        parent.setJsonType(isInArray ? JsonTypeEnum.OBJECT_ARRAY : JsonTypeEnum.OBJECT);

        Collection<PropertyDto> children = Lists.newLinkedList();
        for (Entry<String, JsonSchema> entry : parentSchema.getProperties().entrySet()) {
            String childName = entry.getKey();
            JsonSchema childSchema = entry.getValue();
            PropertyDto child = new PropertyDto();
            child.setName(childName);

            JsonTypeEnum jsonType;
            JsonSchema forCalcJsonFormat = childSchema;
            if (childSchema.isValueTypeSchema()) {
                jsonType = calcValueType(childSchema.asValueTypeSchema(), false);
            } else if (childSchema.isObjectSchema()) {
                jsonType = calcObjectTypeWithRecur(child, childSchema.asObjectSchema(), false);
            } else if (childSchema.isArraySchema()) {
                Items items = childSchema.asArraySchema().getItems();
                if (items == null || items.isArrayItems()) {
                    jsonType = JsonTypeEnum.UNKNOWN;
                } else {
                    JsonSchema eleSchema = items.asSingleItems().getSchema();
                    forCalcJsonFormat = eleSchema;
                    forCalcJsonFormat.setDescription(childSchema.getDescription());
                    if (eleSchema.isValueTypeSchema()) {
                        jsonType = calcValueType(eleSchema.asValueTypeSchema(), true);
                    } else if (eleSchema.isObjectSchema()) {
                        jsonType = calcObjectTypeWithRecur(child, eleSchema.asObjectSchema(), true);
                    } else {
                        jsonType = JsonTypeEnum.UNKNOWN;
                    }
                }
            } else {
                jsonType = JsonTypeEnum.UNKNOWN;
            }
            child.setJsonType(jsonType);

            if (childSchema.getDescription() != null) {
                JsonPropertyDescriptionValueDto jpdv = JsonUtils
                        .toObject(childSchema.getDescription(), JsonPropertyDescriptionValueDto.class);
                child.setDescription(jpdv.getComment());
                child.setValidators(jpdv.getValidators());
                child.setNullable(jpdv.getNullable());
                child.setJsonFormat(calcJsonFormat(jpdv.getRawType(), jpdv.getJsonFormatPattern(), forCalcJsonFormat));
            } else {
                child.setJsonFormat(calcJsonFormat(null, null, forCalcJsonFormat));
                log.warn("Cannot found JsonPropertyDescriptionValue. {}.{}", JsonSchemaUtils.getId(parentSchema),
                        childName);
            }

            child.setParent(parent);
            children.add(child);
        }

        parent.setChildren(children);
        return parent.getJsonType();
    }

    private String calcJsonFormat(String rawType, String jsonFormatPattern, JsonSchema jsonSchema) {
        if (jsonSchema.isValueTypeSchema() && !CollectionUtils.isEmpty(jsonSchema.asValueTypeSchema().getEnums())) {
            StringBuilder sb = new StringBuilder(64);
            try {
                for (String cadJson : jsonSchema.asStringSchema().getEnums()) {
                    CodeAndDescriptionDto cad = JsonUtils.toObject(cadJson, CodeAndDescriptionDto.class);
                    sb.append(cad.getCode()).append("-").append(cad.getDescription());
                    sb.append(",");
                }
            } catch (JsonsException e) {
                jsonSchema.asStringSchema().getEnums().forEach(one -> sb.append(one).append(","));
                log.warn("enum illegal. rawType={}, jsonSchema={}", rawType, jsonSchema);
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            } else {
                sb.append("unknown");
            }
            return String.format(JsonFormatEnum.ENUM.getValue(), sb);
        }

        if (jsonFormatPattern != null && jsonSchema.isStringSchema()) {
            return String.format(JsonFormatEnum.TIME.getValue(), jsonFormatPattern);
        }

        if (jsonSchema.isNumberSchema() && rawType != null) {
            if (!jsonSchema.isIntegerSchema()) {
                return JsonFormatEnum.FLOAT.getValue();
            } else if (StringUtils.containsAny(rawType, "Integer", "int")) {
                return JsonFormatEnum.INT_32.getValue();
            } else if (StringUtils.containsAny(rawType, "Long", "long")) {
                return JsonFormatEnum.INT_64.getValue();
            } else {
                return JsonFormatEnum.INT_UNKNOWN.getValue();
            }
        }

        return JsonFormatEnum.NOTHING_SPECIAL.getValue();
    }

    private JsonTypeEnum calcValueType(ValueTypeSchema vSchema, boolean isInArray) {
        if (vSchema.isNumberSchema()) {
            return isInArray ? JsonTypeEnum.NUMBER_ARRAY : JsonTypeEnum.NUMBER;
        } else if (vSchema.isStringSchema()) {
            return isInArray ? JsonTypeEnum.STRING_ARRAY : JsonTypeEnum.STRING;
        } else if (vSchema.isBooleanSchema()) {
            return isInArray ? JsonTypeEnum.BOOLEAN_ARRAY : JsonTypeEnum.BOOLEAN;
        } else {
            throw new IllegalArgumentException(vSchema.toString());
        }
    }

    private void collectEnumDescription(EnumDeclaration ed, Table<String, String, String> table) {
        String qualifier = ed.getFullyQualifiedName().orElseThrow(QualifierAbsentException::new);
        ed.getEntries()
                .forEach(entry -> table.put(qualifier, entry.getNameAsString(), Javadocs.extractFirstLine(entry)));
    }

    private void collectPropertiesAnnoInfo(ClassOrInterfaceDeclaration coid, Table<String, String, String> table) {
        String javabeanQualifier = coid.getFullyQualifiedName().orElseThrow(QualifierAbsentException::new);
        for (FieldDeclaration field : coid.getFields()) {
            JsonPropertyDescriptionValueDto value = new JsonPropertyDescriptionValueDto();
            value.setComment(Javadocs.extractFirstLine(field));
            value.setNullable(
                    isAnnoAbsent(field, NotNull.class) && isAnnoAbsent(field, NotEmpty.class) && isAnnoAbsent(field,
                            NotBlank.class));
            value.setValidators(new ValidatorProcessor().process(field));

            AnnotationExpr anno = findAnno(field, JsonFormat.class);
            if (anno != null) {
                for (MemberValuePair pair : anno.asNormalAnnotationExpr().getPairs()) {
                    if (pair.getNameAsString().equals("pattern")) {
                        value.setJsonFormatPattern(pair.getValue().asStringLiteralExpr().getValue());
                    }
                }
            }

            for (VariableDeclarator var : field.getVariables()) {
                String variableName = var.getNameAsString();
                try {
                    value.setRawType(var.getTypeAsString());
                } catch (Exception ignored) {
                }
                table.put(javabeanQualifier, variableName, JsonUtils.toJson(value));
            }
        }
    }

    private JsonSchemaGenerator buildJsg(Table<String, String, String> enumDescriptions,
            Table<String, String, String> propertyDescriptions) {
        ObjectMapper om = JsonUtils.initObjectMapper(new ObjectMapper());
        om.setAnnotationIntrospector(new MappingJacksonAnnotationIntrospector(enumDescriptions, propertyDescriptions));
        return new JsonSchemaGenerator(om);
    }

    /**
     * 1. 遍历出声明了@RequestBody的参数后返回
     * 2. 发生任何异常时，都会认为没有ResponseBody
     * 异常均会被log.error，除非目标项目源码更新后没有及时编译，否则不应该抛出异常
     */
    private ResolvedType findRequestBody(MethodDeclaration method) {
        for (Parameter parameter : method.getParameters()) {
            Type type = parameter.getType();
            for (AnnotationExpr annotation : parameter.getAnnotations()) {
                try {
                    ResolvedAnnotationDeclaration resolve = annotation.resolve();
                    if (QualifierConstants.REQUEST_BODY.equals(resolve.getQualifiedName())) {
                        try {
                            return type.resolve();
                        } catch (Exception e) {
                            log.error(e);
                        }
                    }
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
        return null;
    }

    /**
     * 1. controller上没有声明@RestController且handler上没有声明@ResponseBody时，认为没有ResponseBody
     * 2. 采用ConcernedResponseBodyTypeResolver提供的策略来获取ResponseBody
     * 3. 发生任何异常时，都会认为没有ResponseBody
     * 异常均会被log.error，除非目标项目源码更新后没有及时编译，否则不应该抛出异常
     */
    private ResolvedType findResponseBody(ClassOrInterfaceDeclaration controller, MethodDeclaration handler) {
        try {
            if (isRestControllerAnnoAbsent(controller) && isResponseBodyAnnoAbsent(handler)) {
                return null;
            }
            return new ConcernedResponseBodyTypeResolver().findConcernedResponseBodyType(handler);
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    private boolean isDeprecated(ClassOrInterfaceDeclaration controller, MethodDeclaration handler) {
        return isAnnoPresent(handler, Deprecated.class) || isAnnoPresent(controller, Deprecated.class);
    }

    private boolean isResponseBodyAnnoAbsent(MethodDeclaration handler) {
        return isAnnoAbsent(handler, ResponseBody.class);
    }

    private boolean isRestControllerAnnoAbsent(ClassOrInterfaceDeclaration controller) {
        return isAnnoAbsent(controller, RestController.class);
    }

    private <A extends Annotation> boolean isAnnoPresent(NodeWithAnnotations<?> node, Class<A> annotationClass) {
        return findAnno(node, annotationClass) == null;
    }

    private <A extends Annotation> AnnotationExpr findAnno(NodeWithAnnotations<?> node, Class<A> annotationClass) {
        Optional<AnnotationExpr> annotation = node.getAnnotationByName(annotationClass.getSimpleName());
        if (annotation.isPresent()) {
            try {
                if (annotationClass.getName().equals(annotation.get().resolve().getQualifiedName())) {
                    return annotation.get();
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
        return null;
    }

    private <A extends Annotation> boolean isAnnoAbsent(NodeWithAnnotations<?> node, Class<A> annotationClass) {
        return !isAnnoPresent(node, annotationClass);
    }

    private Collection<RequestMethod> combineVerb(RequestMethod[] cVerbs, RequestMethod[] mVerbs) {
        Collection<RequestMethod> combinedVerbs = Lists.newArrayList();
        if (ArrayUtils.isNotEmpty(cVerbs)) {
            combinedVerbs.addAll(Arrays.asList(cVerbs));
        }
        if (ArrayUtils.isNotEmpty(mVerbs)) {
            combinedVerbs.addAll(Arrays.asList(mVerbs));
        }
        if (combinedVerbs.size() == 0) {
            combinedVerbs.addAll(Arrays.asList(RequestMethod.values()));
        }
        return combinedVerbs;
    }

    private Collection<String> combineUrl(String[] cPaths, String[] mPaths) {
        Collection<String> combinedUrls = Lists.newArrayList();
        if (ArrayUtils.isNotEmpty(cPaths) && ArrayUtils.isNotEmpty(mPaths)) {
            for (String cPath : cPaths) {
                for (String mPath : mPaths) {
                    combinedUrls.add(pathMatcher.combine(cPath, mPath));
                }
            }
        } else if (ArrayUtils.isEmpty(cPaths)) {
            combinedUrls.addAll(Arrays.asList(mPaths));
        } else if (ArrayUtils.isEmpty(mPaths)) {
            combinedUrls.addAll(Arrays.asList(cPaths));
        } else {
            combinedUrls.add("/");
        }
        combinedUrls = ensureAllStartWithSlash(combinedUrls);
        return combinedUrls;
    }

    private Collection<String> ensureAllStartWithSlash(Collection<String> urls) {
        Collection<String> result = Lists.newArrayList();
        for (String url : urls) {
            if (!url.startsWith("/")) {
                url = "/" + url;
            }
            result.add(url);
        }
        return result;
    }


    private boolean isNotHandler(Method method) {
        return findRequestMappingAnnoOrElseNull(method) == null;
    }

    private RequestMethod[] findVerbFromAnno(RequestMapping controllerRequestMapping) {
        return controllerRequestMapping == null ? new RequestMethod[0] : controllerRequestMapping.method();
    }

    private String[] findValueFromAnno(RequestMapping controllerRequestMapping) {
        return controllerRequestMapping == null ? new String[0] : controllerRequestMapping.value();
    }

    private RequestMapping findRequestMappingAnnoOrElseNull(AnnotatedElement controllerClass) {
        return AnnotatedElementUtils.findMergedAnnotation(controllerClass, RequestMapping.class);
    }

    private Class<?> tryReflectController(ClassOrInterfaceDeclaration controller) throws ClassNotFoundException {
        return LoadClassUtils.loadClass(controller.getFullyQualifiedName().orElseThrow(QualifierAbsentException::new),
                astForest.getCurrentClassLoader());
    }

    private boolean isController(ClassOrInterfaceDeclaration coid) {
        for (AnnotationExpr annotation : coid.getAnnotations()) {
            try {
                ResolvedAnnotationDeclaration resolve = annotation.resolve();
                if (resolve.hasAnnotation(QualifierConstants.CONTROLLER) || QualifierConstants.CONTROLLER
                        .equals(resolve.getName())) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("annotation [{}] of class [{}] cannot resolve", annotation.getNameAsString(),
                        coid.getNameAsString(), e);
            }
        }
        return false;
    }

    @Data
    private static class JsonPropertyDescriptionValue {

        String comment;

        Boolean nullable;

        Collection<PropertyValidatorDto> validators;

        String rawType;

    }

}
