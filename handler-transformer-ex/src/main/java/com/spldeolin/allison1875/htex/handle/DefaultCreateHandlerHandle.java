package com.spldeolin.allison1875.htex.handle;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.VoidType;
import com.spldeolin.allison1875.base.builder.ServiceCuBuilder;
import com.spldeolin.allison1875.base.constant.AnnotationConstant;
import com.spldeolin.allison1875.htex.javabean.FirstLineDto;

/**
 * @author Deolin 2021-01-11
 */
public class DefaultCreateHandlerHandle implements CreateHandlerHandle {

    @Override
    public MethodDeclaration createHandler(FirstLineDto firstLineDto, String serviceParamType, String serviceResultType,
            ServiceCuBuilder serviceCuBuilder) {
        MethodDeclaration handler = new MethodDeclaration();
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
            parameter.setType(serviceParamType);
            parameter.setName("req");
            handler.addParameter(parameter);
        }

        BlockStmt body = new BlockStmt();
        if (serviceParamType != null) {
            body.addStatement(StaticJavaParser.parseStatement(
                    String.format("return %s.%s(req);", serviceCuBuilder.getServiceVarName(),
                            serviceCuBuilder.getMethodName())));
        }
        handler.setBody(body);
        return handler;
    }

}