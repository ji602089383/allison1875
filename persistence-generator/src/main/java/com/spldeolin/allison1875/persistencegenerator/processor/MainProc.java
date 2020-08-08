package com.spldeolin.allison1875.persistencegenerator.processor;

import java.util.Collection;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.google.common.collect.Lists;
import com.spldeolin.allison1875.base.creator.CuCreator;
import com.spldeolin.allison1875.base.util.ast.Saves;
import com.spldeolin.allison1875.persistencegenerator.PersistenceGeneratorConfig;
import com.spldeolin.allison1875.persistencegenerator.javabean.PersistenceDto;
import com.spldeolin.allison1875.persistencegenerator.processor.mapper.InsertProcessor;
import com.spldeolin.allison1875.persistencegenerator.processor.mapper.QueryByFkProcessor;
import com.spldeolin.allison1875.persistencegenerator.processor.mapper.QueryByPkProcessor;
import com.spldeolin.allison1875.persistencegenerator.processor.mapper.QueryByPksEachPkProcessor;
import com.spldeolin.allison1875.persistencegenerator.processor.mapper.QueryByPksProcessor;
import com.spldeolin.allison1875.persistencegenerator.processor.mapper.UpdateByPkEvenNullProcessor;
import com.spldeolin.allison1875.persistencegenerator.processor.mapper.UpdateByPkProcessor;
import com.spldeolin.allison1875.persistencegenerator.processor.mapperxml.ResultMapXmlProc;
import com.spldeolin.allison1875.persistencegenerator.processor.mapperxml.AllCloumnSqlXmlProc;
import com.spldeolin.allison1875.persistencegenerator.processor.mapperxml.InsertXmlProc;
import com.spldeolin.allison1875.persistencegenerator.processor.mapperxml.QueryByFkXmlProc;
import com.spldeolin.allison1875.persistencegenerator.processor.mapperxml.QueryByPkXmlProc;
import com.spldeolin.allison1875.persistencegenerator.processor.mapperxml.QueryByPksXmlProc;
import com.spldeolin.allison1875.persistencegenerator.processor.mapperxml.UpdateByPkEvenNullXmlProc;
import com.spldeolin.allison1875.persistencegenerator.processor.mapperxml.UpdateByPkXmlProc;
import lombok.extern.log4j.Log4j2;

/**
 * @author Deolin 2020-07-11
 */
@Log4j2
public class MainProc {


    /**
     * @see adfa
     */
    public static void main(String[] args) {
        Collection<CompilationUnit> toSave = Lists.newArrayList();

        // 构建并遍历 PersistenceDto对象
        for (PersistenceDto persistence : new BuildPersistenceDtoProc().process().getPersistences()) {

            // 重新生成Entity
            EntityProc entityProcessor = new EntityProc(persistence).process();
            CuCreator entityCuCreator = entityProcessor.getEntityCuCreator();
            toSave.add(entityCuCreator.create(false));

            // 寻找或创建Mapper
            ClassOrInterfaceDeclaration mapper;
            try {
                FindOrCreateMapperProc processor = new FindOrCreateMapperProc(persistence, entityCuCreator).process();
                mapper = processor.getMapper();
                toSave.add(processor.getCu());
            } catch (Exception e) {
                log.error("寻找或创建Mapper时发生异常 persistence={}", persistence, e);
                continue;
            }

            // 在Mapper中生成基础方法
            new InsertProcessor(persistence, mapper).process();
            new QueryByPkProcessor(persistence, mapper).process();
            new UpdateByPkProcessor(persistence, mapper).process();
            new UpdateByPkEvenNullProcessor(persistence, mapper).process();
            new QueryByPkProcessor(persistence, mapper).process();
            new QueryByPksEachPkProcessor(persistence, mapper).process();
            new QueryByPksProcessor(persistence, mapper).process();
            new QueryByFkProcessor(persistence, mapper).process();

            // 在Mapper.xml中生成基础方法
            String entityName = getEntityNameInXml(entityCuCreator);
            try {
                new MapperXmlProc(persistence, mapper, new ResultMapXmlProc(persistence, entityName).process(),
                        new AllCloumnSqlXmlProc(persistence).process(),
                        new InsertXmlProc(persistence, entityName).process(),
                        new UpdateByPkXmlProc(persistence, entityName).process(),
                        new UpdateByPkEvenNullXmlProc(persistence, entityName).process(),
                        new QueryByPkXmlProc(persistence).process(),
                        new QueryByPksXmlProc(persistence, "queryByIds").process(),
                        new QueryByPksXmlProc(persistence, "queryByIdsEachId").process(),
                        new QueryByFkXmlProc(persistence).process()).process();
            } catch (Exception e) {
                log.error("写入Mapper.xml时发生异常 persistence={}", persistence, e);
            }

        }

        toSave.forEach(Saves::prettySave);
    }

    private static String getEntityNameInXml(CuCreator entityCuCreator) {
        if (PersistenceGeneratorConfig.getInstace().getIsEntityUsingAlias()) {
            return entityCuCreator.getPrimaryTypeName();
        } else {
            return entityCuCreator.getPrimaryTypeQualifier();
        }
    }

}