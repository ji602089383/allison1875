package com.spldeolin.allison1875.persistencegenerator.processor;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.apache.logging.log4j.Logger;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.JavadocBlockTag.Type;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.spldeolin.allison1875.base.constant.BaseConstant;
import com.spldeolin.allison1875.base.creator.CuCreator;
import com.spldeolin.allison1875.base.exception.QualifierAbsentException;
import com.spldeolin.allison1875.base.util.JsonUtils;
import com.spldeolin.allison1875.base.util.StringUtils;
import com.spldeolin.allison1875.persistencegenerator.PersistenceGeneratorConfig;
import com.spldeolin.allison1875.persistencegenerator.javabean.PersistenceDto;
import com.spldeolin.allison1875.persistencegenerator.javabean.PropertyDto;
import com.spldeolin.allison1875.persistencegenerator.javabean.QueryMeta;

/**
 * @author Deolin 2020-10-06
 */
public class QueryDesignProc {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(EntityProc.class);

    private final PersistenceDto persistence;

    private final CuCreator entityCuCreator;

    private final ClassOrInterfaceDeclaration mapper;

    public QueryDesignProc(PersistenceDto persistence, CuCreator entityCuCreator, ClassOrInterfaceDeclaration mapper) {
        this.persistence = persistence;
        this.entityCuCreator = entityCuCreator;
        this.mapper = mapper;
    }

    public QueryDesignProc process() {
        PersistenceGeneratorConfig conf = PersistenceGeneratorConfig.getInstance();
        if (!conf.getEnableGenerateQueryDesign()) {
            return this;
        }

        Path queryPath = CodeGenerationUtils
                .fileInPackageAbsolutePath(entityCuCreator.getSourceRoot(), conf.getEntityPackage(),
                        persistence.getEntityName() + ".java");

        List<JavadocBlockTag> authorTags = Lists.newArrayList();
        if (queryPath.toFile().exists()) {
            try {
                CompilationUnit cu = StaticJavaParser.parse(queryPath);
                this.getAuthorTags(authorTags, cu);
            } catch (Exception e) {
                log.warn("StaticJavaParser.parse failed entityPath={}", queryPath, e);
            }
            log.info("Query文件已存在，覆盖它。 [{}]", queryPath);
        } else {
            authorTags.add(new JavadocBlockTag(Type.AUTHOR, conf.getAuthor() + " " + LocalDate.now()));
        }

        List<String> imports = this.getImports(persistence, conf);

        CuCreator cuCreator = new CuCreator(entityCuCreator.getSourceRoot(), conf.getQueryDesignPackage(), imports,
                () -> {
                    ClassOrInterfaceDeclaration coid = new ClassOrInterfaceDeclaration();
                    Javadoc classJavadoc = new JavadocComment(
                            persistence.getDescrption() + BaseConstant.NEW_LINE + "<p>" + persistence.getTableName()
                                    + Strings.repeat(BaseConstant.NEW_LINE, 2) + "<p><p>" + "<strong>该类型"
                                    + BaseConstant.BY_ALLISON_1875 + "</strong>").parse();
                    classJavadoc.getBlockTags().addAll(authorTags);
                    coid.setJavadocComment(classJavadoc);
                    coid.setPublic(true);
                    coid.setName(calcQueryDesignName(conf));
                    setDefaultConstructorPrivate(coid);
                    addStaticFactory(coid);
                    for (PropertyDto property : persistence.getProperties()) {
                        addIntermediateField(coid, property);
                    }
                    addTerminalMethod(coid, persistence);

                    QueryMeta queryMeta = new QueryMeta();
                    queryMeta.setEntityQualifier(entityCuCreator.getPrimaryTypeQualifier());
                    queryMeta.setEntityName(entityCuCreator.getPrimaryTypeName());
                    queryMeta.setMapperQualifier(
                            mapper.getFullyQualifiedName().orElseThrow(QualifierAbsentException::new));
                    queryMeta.setMapperName(mapper.getNameAsString());
                    queryMeta.setMapperRelativePath(
                            PersistenceGeneratorConfig.getInstance().getMapperXmlDirectoryPath() + File.separator
                                    + persistence.getMapperName());
                    coid.addOrphanComment(new LineComment(JsonUtils.toJson(queryMeta)));
                    return coid;
                });
        cuCreator.create(true);

        return this;
    }

    private void addTerminalMethod(ClassOrInterfaceDeclaration coid, PersistenceDto persistence) {
        MethodDeclaration method = new MethodDeclaration();
        method.setPublic(true);
        method.setType(StaticJavaParser.parseType(String.format("List<%s>", persistence.getEntityName())));
        method.setName("over");
        method.setBody(new BlockStmt().addStatement("throw new UnsupportedOperationException();"));
        coid.addMember(method);
    }

    private void addIntermediateField(ClassOrInterfaceDeclaration coid, PropertyDto property) {
        FieldDeclaration field = new FieldDeclaration();
        field.setPublic(true);
        com.github.javaparser.ast.type.Type type = StaticJavaParser.parseType(
                String.format("QueryPredicate<%s, %s>", coid.getName(), property.getJavaType().getSimpleName()));
        VariableDeclarator variable = new VariableDeclarator(type, property.getPropertyName());
        field.addVariable(variable);
        Javadoc fieldJavadoc = new JavadocComment(buildCommentDescription(property)).parse();
        field.setJavadocComment(fieldJavadoc);
        coid.addMember(field);
    }

    private String buildCommentDescription(PropertyDto property) {
        String result = property.getDescription();
        result += BaseConstant.NEW_LINE + "<p>" + property.getColumnName();
        if (property.getLength() != null) {
            result += BaseConstant.NEW_LINE + "<p>长度：" + property.getLength();
        }
        if (property.getNotnull()) {
            result += BaseConstant.NEW_LINE + "<p>不能为null";
        }
        if (property.getDefaultV() != null) {
            String defaultV = property.getDefaultV();
            if (!"CURRENT_TIMESTAMP".equals(defaultV)) {
                defaultV = "'" + defaultV + "'";
            }
            result += BaseConstant.NEW_LINE + "<p>默认：" + defaultV;
        }
        return result;
    }

    private void addStaticFactory(ClassOrInterfaceDeclaration coid) {
        MethodDeclaration method = new MethodDeclaration();
        method.setPublic(true);
        method.setStatic(true);
        method.setType(coid.getNameAsString());
        method.setName("design");
        method.addParameter("String", "methodName");
        method.setBody(new BlockStmt().addStatement("throw new UnsupportedOperationException(methodName);"));
        coid.addMember(method);
    }

    private void setDefaultConstructorPrivate(ClassOrInterfaceDeclaration coid) {
        ConstructorDeclaration constructor = new ConstructorDeclaration();
        constructor.setPrivate(true);
        constructor.setName(coid.getName());
        constructor.setBody(new BlockStmt());
        coid.addMember(constructor);
    }

    private String calcQueryDesignName(PersistenceGeneratorConfig conf) {
        return conf.getIsEntityEndWithEntity() ? StringUtils.replaceLast(persistence.getEntityName(), "Entity", "Query")
                : persistence.getEntityName() + "QueryDesign";
    }

    private List<String> getImports(PersistenceDto persistence, PersistenceGeneratorConfig conf) {
        List<String> result = Lists.newArrayList();
        for (PropertyDto property : persistence.getProperties()) {
            String qualifier = property.getJavaType().getName();
            if (!qualifier.startsWith("java.lang")) {
                result.add(qualifier);
            }
        }
        result.add(conf.getQueryPredicateQualifier());
        result.add("java.util.List");
        result.add(entityCuCreator.getPrimaryTypeQualifier());
        result.sort(String::compareTo);
        return result;
    }

    private void getAuthorTags(List<JavadocBlockTag> authorTags, CompilationUnit cu) {
        cu.getPrimaryType()
                .ifPresent(pt -> pt.getJavadoc().ifPresent(javadoc -> javadoc.getBlockTags().forEach(javadocTag -> {
                    if (javadocTag.getType() == Type.AUTHOR) {
                        authorTags.add(javadocTag);
                    }
                })));
    }

}