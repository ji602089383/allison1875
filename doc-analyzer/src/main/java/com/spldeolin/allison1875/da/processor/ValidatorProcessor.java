package com.spldeolin.allison1875.da.processor;

import java.util.Collection;
import org.apache.commons.lang3.StringUtils;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.google.common.collect.Lists;
import com.spldeolin.allison1875.da.dto.ValidatorDto;
import com.spldeolin.allison1875.da.enums.ValidatorTypeEnum;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

/**
 * 校验项
 *
 * @author Deolin 2019-12-09
 */
@Log4j2
@Accessors(fluent = true)
public class ValidatorProcessor {

    public Collection<ValidatorDto> process(NodeWithAnnotations<?> node) {
        Collection<ValidatorDto> result = Lists.newLinkedList();
        for (AnnotationExpr annotation : node.getAnnotations()) {
            ResolvedAnnotationDeclaration resolve = annotation.resolve();
            String qualifier = resolve.getQualifiedName();
            if (StringUtils.equalsAny(qualifier, "javax.validation.constraints.NotEmpty",
                    "org.hibernate.validator.constraints.NotEmpty")) {
                result.add(new ValidatorDto().setValidatorType(ValidatorTypeEnum.notEmpty.getValue()));
            } else if (StringUtils.equalsAny(qualifier, "javax.validation.constraints.NotBlank",
                    "org.hibernate.validator.constraints.NotBlank")) {
                result.add(new ValidatorDto().setValidatorType(ValidatorTypeEnum.notBlank.getValue()));
            } else if (StringUtils.equalsAny(qualifier, "javax.validation.constraints.Size",
                    "org.hibernate.validator.constraints.Length")) {
                annotation.asNormalAnnotationExpr().getPairs().forEach(pair -> {
                    ValidatorDto validator = new ValidatorDto().setNote(pair.getValue().toString());
                    if (nameOf(pair, "min")) {
                        result.add(validator.setValidatorType(ValidatorTypeEnum.minSize.getValue()));
                    }
                    if (nameOf(pair, "max")) {
                        result.add(validator.setValidatorType(ValidatorTypeEnum.maxSize.getValue()));
                    }
                });
            } else if (StringUtils.equalsAny(qualifier, "javax.validation.constraints.Max")) {
                annotation.ifSingleMemberAnnotationExpr(singleAnno -> result
                        .add(new ValidatorDto().setValidatorType(ValidatorTypeEnum.maxInteger.getValue())
                                .setNote(singleAnno.getMemberValue().toString())));
                annotation.ifNormalAnnotationExpr(
                        normalAnno -> normalAnno.getPairs().stream().filter(this::nameIsValue).findAny().ifPresent(
                                pair -> result.add(new ValidatorDto()
                                        .setValidatorType(ValidatorTypeEnum.maxInteger.getValue())
                                        .setNote(pair.getValue().toString()))));
            } else if (StringUtils.equalsAny(qualifier, "javax.validation.constraints.Min")) {
                annotation.ifSingleMemberAnnotationExpr(singleAnno -> result
                        .add(new ValidatorDto().setValidatorType(ValidatorTypeEnum.minInteger.getValue())
                                .setNote(singleAnno.getMemberValue().toString())));
                annotation.ifNormalAnnotationExpr(
                        normalAnno -> normalAnno.getPairs().stream().filter(this::nameIsValue).findAny().ifPresent(
                                pair -> result.add(new ValidatorDto()
                                        .setValidatorType(ValidatorTypeEnum.minInteger.getValue())
                                        .setNote(pair.getValue().toString()))));
            } else if (StringUtils.equalsAny(qualifier, "javax.validation.constraints.DecimalMax")) {
                annotation.ifSingleMemberAnnotationExpr(singleAnno -> result
                        .add(new ValidatorDto().setValidatorType(ValidatorTypeEnum.maxFloat.getValue())
                                .setNote(singleAnno.getMemberValue().toString())));
                annotation.ifNormalAnnotationExpr(
                        normalAnno -> normalAnno.getPairs().stream().filter(this::nameIsValue).findAny().ifPresent(
                                pair -> result
                                        .add(new ValidatorDto().setValidatorType(ValidatorTypeEnum.maxFloat.getValue())
                                                .setNote(pair.getValue().toString()))));
            } else if (StringUtils.equalsAny(qualifier, "javax.validation.constraints.DecimalMin")) {
                annotation.ifSingleMemberAnnotationExpr(singleAnno -> result
                        .add(new ValidatorDto().setValidatorType(ValidatorTypeEnum.minFloat.getValue())
                                .setNote(singleAnno.getMemberValue().toString())));
                annotation.ifNormalAnnotationExpr(
                        normalAnno -> normalAnno.getPairs().stream().filter(this::nameIsValue).findAny().ifPresent(
                                pair -> result
                                        .add(new ValidatorDto().setValidatorType(ValidatorTypeEnum.minFloat.getValue())
                                                .setNote(pair.getValue().toString()))));
            } else if (StringUtils.equalsAny(qualifier, "javax.validation.constraints.Future")) {
                result.add(new ValidatorDto().setValidatorType(ValidatorTypeEnum.future.getValue()));
            } else if (StringUtils.equalsAny(qualifier, "javax.validation.constraints.Past")) {
                result.add(new ValidatorDto().setValidatorType(ValidatorTypeEnum.past.getValue()));
            } else if (StringUtils.equalsAny(qualifier, "javax.validation.constraints.Digits")) {
                annotation.asNormalAnnotationExpr().getPairs().forEach(pair -> {
                    ValidatorDto validator = new ValidatorDto().setNote(pair.getValue().toString());
                    if (nameOf(pair, "integer")) {
                        result.add(validator.setValidatorType(ValidatorTypeEnum.maxIntegralDigits.getValue()));
                    }
                    if (nameOf(pair, "fraction")) {
                        result.add(validator.setValidatorType(ValidatorTypeEnum.maxFractionalDigits.getValue()));
                    }
                });
            } else if (StringUtils.equalsAny(qualifier, "javax.validation.constraints.Positive")) {
                result.add(new ValidatorDto().setValidatorType(ValidatorTypeEnum.positive.getValue()));
            } else if (StringUtils.equalsAny(qualifier, "javax.validation.constraints.Negative")) {
                result.add(new ValidatorDto().setValidatorType(ValidatorTypeEnum.negative.getValue()));
            } else if (StringUtils.equalsAny(qualifier, "javax.validation.constraints.Pattern")) {
                annotation.asNormalAnnotationExpr().getPairs().forEach(pair -> {
                    if (nameOf(pair, "regexp")) {
                        result.add(new ValidatorDto().setValidatorType(ValidatorTypeEnum.regex.getValue())
                                .setNote(pair.getValue().asStringLiteralExpr().asString()));
                    }
                });
            }
        }
        return result;
    }

    private boolean nameIsValue(MemberValuePair pair) {
        return nameOf(pair, "value");
    }

    private boolean nameOf(MemberValuePair pair, String value) {
        return pair.getNameAsString().equals(value);
    }

}