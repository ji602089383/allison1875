package com.spldeolin.allison1875.persistencegenerator.processor.mapper;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.javadoc.Javadoc;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.spldeolin.allison1875.persistencegenerator.PersistenceGeneratorConfig;
import com.spldeolin.allison1875.persistencegenerator.javabean.PersistenceDto;

/**
 * 批量更新
 *
 * @author Deolin 2020-12-18
 */
@Singleton
public class BatchUpdateEvenNullProc extends MapperProc {

    @Inject
    private PersistenceGeneratorConfig persistenceGeneratorConfig;

    public String process(PersistenceDto persistence, ClassOrInterfaceDeclaration mapper) {
        if (persistenceGeneratorConfig.getDisableBatchUpdateEvenNull()) {
            return null;
        }

        String methodName = super.calcMethodName(mapper, "batchUpdateEvenNull");
        MethodDeclaration update = new MethodDeclaration();
        String lotNoText = getLotNoText(persistenceGeneratorConfig, persistence);
        Javadoc javadoc = new JavadocComment("批量根据ID更新数据，为null对应的字段会被更新为null" + lotNoText).parse();
        update.setJavadocComment(javadoc);
        update.setType(PrimitiveType.intType());
        update.setName(methodName);
        update.addParameter(StaticJavaParser.parseParameter(
                "@Param(\"entities\") Collection<" + persistence.getEntityName() + "> entities"));
        update.setBody(null);
        mapper.getMembers().addLast(update);
        return methodName;
    }

}