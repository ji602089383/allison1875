package com.spldeolin.allison1875.handlertransformer.processor;

import java.util.Collection;
import org.apache.commons.lang3.tuple.Pair;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.google.common.collect.Sets;
import com.spldeolin.allison1875.base.ancestor.Allison1875MainProc;
import com.spldeolin.allison1875.base.collection.ast.AstForest;
import com.spldeolin.allison1875.base.collection.ast.AstForestContext;
import com.spldeolin.allison1875.base.util.ast.Saves;
import com.spldeolin.allison1875.handlertransformer.javabean.MetaInfo;

/**
 * @author Deolin 2020-06-22
 */
public class HandlerTransformer implements Allison1875MainProc {

    @Override
    public void process(AstForest astForest) {
        AstForestContext.setCurrent(astForest);
        Collection<CompilationUnit> cus = Sets.newHashSet();

        for (CompilationUnit cu : AstForestContext.getCurrent()) {
            for (Pair<ClassOrInterfaceDeclaration, InitializerDeclaration> pair : new BlueprintCollectProc(cu).process()
                    .getControllerAndBlueprints()) {

                ClassOrInterfaceDeclaration controller = pair.getLeft();
                InitializerDeclaration blueprint = pair.getRight();

                BlueprintAnalyzeProc blueprintAnalyzeProc = new BlueprintAnalyzeProc(controller, blueprint).process();
                MetaInfo metaInfo = blueprintAnalyzeProc.getMetaInfo();

                if (metaInfo.isLack()) {
                    continue;
                }

                GenerateDtosProc generateDtosProc = new GenerateDtosProc(metaInfo.getSourceRoot(), metaInfo.getDtos())
                        .process();
                cus.addAll(generateDtosProc.getDtoCus());

                GenerateServicesProc generateServicesProc = new GenerateServicesProc(metaInfo).process();
                cus.add(generateServicesProc.getServiceCu());
                cus.add(generateServicesProc.getServiceImplCu());

                GenerateHandlerProc generateHandlerProc = new GenerateHandlerProc(metaInfo,
                        generateServicesProc.getServiceQualifier()).process();
                cus.add(generateHandlerProc.getControllerCu());
            }
        }

        Saves.prettySave(cus);
    }

}
