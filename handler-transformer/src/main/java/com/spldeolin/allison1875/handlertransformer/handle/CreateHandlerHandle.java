package com.spldeolin.allison1875.handlertransformer.handle;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.VoidType;
import com.google.inject.Singleton;
import com.spldeolin.allison1875.base.constant.AnnotationConstant;
import com.spldeolin.allison1875.handlertransformer.handle.javabean.HandlerCreation;
import com.spldeolin.allison1875.handlertransformer.javabean.FirstLineDto;
import com.spldeolin.allison1875.handlertransformer.javabean.ServiceGeneration;

/**
 * @author Deolin 2021-01-11
 */
@Singleton
public class CreateHandlerHandle {

    public HandlerCreation createHandler(FirstLineDto firstLineDto, String serviceParamType, String serviceResultType,
            ServiceGeneration serviceGeneration) {
        MethodDeclaration handler = new MethodDeclaration();

        handler.setJavadocComment(
                firstLineDto.getHandlerDescription() + firstLineDto.getLotNo().asJavadocDescription());

        handler.addAnnotation(
                StaticJavaParser.parseAnnotation(String.format("@PostMapping(\"%s\")", firstLineDto.getHandlerUrl())));
        handler.setPublic(true);
        if (serviceResultType != null) {
            handler.setType(serviceResultType);
        } else {
            handler.setType(new VoidType());
        }
        handler.setName(firstLineDto.getHandlerName());
        if (serviceParamType != null) {
            Parameter parameter = new Parameter();
            parameter.addAnnotation(AnnotationConstant.REQUEST_BODY);
            parameter.addAnnotation(AnnotationConstant.VALID);
            parameter.setType(serviceParamType);
            parameter.setName("req");
            handler.addParameter(parameter);
        }

        BlockStmt body = new BlockStmt();
        if (serviceResultType != null) {
            if (serviceParamType != null) {
                body.addStatement(StaticJavaParser.parseStatement(
                        String.format("return %s.%s(req);", serviceGeneration.getServiceVarName(),
                                serviceGeneration.getMethodName())));
            } else {
                body.addStatement(StaticJavaParser.parseStatement(
                        String.format("return %s.%s();", serviceGeneration.getServiceVarName(),
                                serviceGeneration.getMethodName())));
            }
        } else {
            if (serviceParamType != null) {
                body.addStatement(StaticJavaParser.parseStatement(
                        String.format("%s.%s(req);", serviceGeneration.getServiceVarName(),
                                serviceGeneration.getMethodName())));
            } else {
                body.addStatement(StaticJavaParser.parseStatement(
                        String.format("%s.%s();", serviceGeneration.getServiceVarName(),
                                serviceGeneration.getMethodName())));
            }
        }
        handler.setBody(body);

        return new HandlerCreation().setHandler(handler);
    }

}