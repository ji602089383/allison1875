package com.spldeolin.allison1875.startransformer.processor;

import org.apache.commons.lang3.StringUtils;
import org.atteo.evo.inflector.English;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.google.inject.Singleton;
import com.spldeolin.allison1875.base.util.CollectionUtils;
import com.spldeolin.allison1875.base.util.MoreStringUtils;
import com.spldeolin.allison1875.startransformer.javabean.ChainAnalysisDto;
import com.spldeolin.allison1875.startransformer.javabean.PhraseDto;

/**
 * @author Deolin 2023-05-22
 */
@Singleton
public class TransformChainProc {

    public void transformAndReplaceStar(BlockStmt block, ChainAnalysisDto analysis, MethodCallExpr starChain) {
        int i = block.getStatements().indexOf(starChain.findAncestor(Statement.class).get());
        block.setStatement(i, StaticJavaParser.parseStatement(
                analysis.getWholeDtoName() + " whole = new " + analysis.getWholeDtoName() + "();"));
        block.addStatement(++i, StaticJavaParser.parseStatement(
                analysis.getCftEntityName() + " " + entityNameToVarName(analysis.getCftEntityName()) + " = "
                        + analysis.getCftDesignName() + "." + "query().byForced().id.eq("
                        + analysis.getCftSecondArgument() + ").one();"));
        for (PhraseDto phrase : analysis.getPhrases()) {
            String code;
            if (phrase.getIsOneToOne()) {
                code = phrase.getDtEntityName() + " " + entityNameToVarName(phrase.getDtEntityName());
            } else {
                code = "List<" + phrase.getDtEntityName() + "> " + English.plural(
                        entityNameToVarName(phrase.getDtEntityName()));
            }
            code += " = " + phrase.getDtDesignName() + ".query().byForced()." + phrase.getFk() + ".eq("
                    + entityNameToVarName(analysis.getCftEntityName()) + ".getId())";
            if (phrase.getIsOneToOne()) {
                code += ".one();";
            } else {
                code += ".many();";
            }
            block.addStatement(++i, StaticJavaParser.parseStatement(code));
            NodeList<Statement> stmtsInForBlock = new NodeList<>();
            for (String key : phrase.getKeys()) {
                String mapVarName =
                        English.plural(entityNameToVarName(phrase.getDtEntityName())) + "Each" + StringUtils.capitalize(
                                key);
                block.addStatement(++i, StaticJavaParser.parseStatement(
                        "Map<" + phrase.getEntityFieldTypesEachFieldName().get(key) + ", " + phrase.getDtEntityName()
                                + "> " + mapVarName + " = Maps.newLinkedHashMap();"));
                stmtsInForBlock.add(StaticJavaParser.parseStatement(
                        mapVarName + ".put(" + entityNameToVarName(phrase.getDtEntityName()) + ".get"
                                + StringUtils.capitalize(key) + "(), " + entityNameToVarName(phrase.getDtEntityName())
                                + ");"));
            }
            for (String mkey : phrase.getMkeys()) {
                String mapVarName =
                        English.plural(entityNameToVarName(phrase.getDtEntityName())) + "Each" + StringUtils.capitalize(
                                mkey);
                block.addStatement(++i, StaticJavaParser.parseStatement(
                        "Multimap<" + phrase.getEntityFieldTypesEachFieldName().get(mkey) + ", "
                                + phrase.getDtEntityName() + "> " + mapVarName + " = LinkedListMultimap.create();"));
                stmtsInForBlock.add(StaticJavaParser.parseStatement(
                        mapVarName + ".put(" + entityNameToVarName(phrase.getDtEntityName()) + ".get"
                                + StringUtils.capitalize(mkey) + "(), " + entityNameToVarName(phrase.getDtEntityName())
                                + ");"));
            }
            if (CollectionUtils.isNotEmpty(stmtsInForBlock)) {
                ForEachStmt forEach = new ForEachStmt();
                forEach.setVariable(new VariableDeclarationExpr(StaticJavaParser.parseType(phrase.getDtEntityName()),
                        entityNameToVarName(phrase.getDtEntityName())));
                forEach.setIterable(new NameExpr(English.plural(entityNameToVarName(phrase.getDtEntityName()))));
                forEach.setBody(new BlockStmt(stmtsInForBlock));
                block.addStatement(++i, forEach);
            }
        }
        block.addStatement(++i, StaticJavaParser.parseStatement(
                "whole." + CodeGenerationUtils.setterName(entityNameToVarName(analysis.getCftEntityName())) + "("
                        + entityNameToVarName(analysis.getCftEntityName()) + ");"));
        for (PhraseDto phrase : analysis.getPhrases()) {
            String dtVarName = English.plural(entityNameToVarName(phrase.getDtEntityName()),
                    phrase.getIsOneToOne() ? 1 : 2);
            block.addStatement(++i, StaticJavaParser.parseStatement(
                    "whole." + CodeGenerationUtils.setterName(dtVarName) + "(" + dtVarName + ");"));
            for (String key : phrase.getKeys()) {
                block.addStatement(++i, StaticJavaParser.parseStatement(
                        "whole." + CodeGenerationUtils.setterName(dtVarName) + "Each" + StringUtils.capitalize(key)
                                + "(" + dtVarName + "Each" + StringUtils.capitalize(key) + ");"));
            }
            for (String mkey : phrase.getMkeys()) {
                block.addStatement(++i, StaticJavaParser.parseStatement(
                        "whole." + CodeGenerationUtils.setterName(dtVarName) + "Each" + StringUtils.capitalize(mkey)
                                + "(" + dtVarName + "Each" + StringUtils.capitalize(mkey) + ");"));
            }
        }
    }

    private String entityNameToVarName(String entityName) {
        return MoreStringUtils.lowerFirstLetter(StringUtils.removeEnd(entityName, "Entity"));
    }

}