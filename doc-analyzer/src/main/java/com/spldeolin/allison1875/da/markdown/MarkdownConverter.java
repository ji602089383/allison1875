package com.spldeolin.allison1875.da.markdown;

import java.io.File;
import java.util.Collection;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.spldeolin.allison1875.base.exception.FreeMarkerPrintExcpetion;
import com.spldeolin.allison1875.base.util.StringUtils;
import com.spldeolin.allison1875.da.DocAnalyzerConfig;
import com.spldeolin.allison1875.da.dto.EndpointDto;
import com.spldeolin.allison1875.da.dto.PropertyDto;
import com.spldeolin.allison1875.da.dto.PropertyValidatorDto;
import com.spldeolin.allison1875.da.showdoc.ShowdocHttpInvoker;
import lombok.extern.log4j.Log4j2;

/**
 * @author Deolin 2020-02-18
 */
@Log4j2
public class MarkdownConverter {

    private static final String horizontalLine = "-";

    public void convert(Collection<EndpointDto> endpoints, boolean alsoSendToShowdoc) {
        int sequence = 0;
        for (EndpointDto endpoint : endpoints) {
            EndpointVo vo = new EndpointVo();
            // 概要
            vo.setUri(Joiner.on("\n").join(endpoint.getUrls()));
            vo.setHttpMethod(Joiner.on("\n").join(endpoint.getHttpMethods()));
            vo.setDescription(endpoint.getDescription());

            // 参数结构
            vo.setRequestBodySituation(endpoint.getRequestBodySituation().getValue());
            vo.setRequestBodyJsonSchema(endpoint.getRequestBodyJsonSchema());
            if (endpoint.getRequestBodyProperties() != null) {
                Collection<RequestBodyPropertyVo> propVos = Lists.newArrayList();
                for (PropertyDto dto : endpoint.getRequestBodyProperties()) {
                    RequestBodyPropertyVo propVo = new RequestBodyPropertyVo();
                    propVo.setPath(dto.getPath());
                    propVo.setName(dto.getName());
                    propVo.setDescription(emptyToHorizontalLine(dto.getDescription()));
                    propVo.setJsonTypeAndFormats(dto.getJsonType().getValue() + " " + dto.getJsonFormat());
                    propVo.setValidators(convertValidators(dto.getRequired(), dto.getValidators()));
                    propVos.add(propVo);
                }
                vo.setRequestBodyProperties(propVos);
                vo.setAnyValidatorsExist(propVos.stream().anyMatch(one -> !horizontalLine.equals(one.getValidators())));
            }

            // 返回值结构
            vo.setResponseBodySituation(endpoint.getResponseBodySituation().getValue());
            vo.setResponseBodyJsonSchema(endpoint.getResponseBodyJsonSchema());
            if (endpoint.getResponseBodyProperties() != null) {
                Collection<ResponseBodyPropertyVo> propVos = Lists.newArrayList();
                for (PropertyDto dto : endpoint.getResponseBodyProperties()) {
                    ResponseBodyPropertyVo propVo = new ResponseBodyPropertyVo();
                    propVo.setPath(dto.getPath());
                    propVo.setName(dto.getName());
                    propVo.setDescription(emptyToHorizontalLine(dto.getDescription()));
                    propVo.setJsonTypeAndFormats(dto.getJsonType().getValue() + " " + dto.getJsonFormat());
                    propVos.add(propVo);
                }
                vo.setResponseBodyProperties(propVos);
            }

            vo.setAuthor(endpoint.getAuthor());
            vo.setSourceCode(endpoint.getSourceCode());

            String uriFirstLine = StringUtils.splitLineByLine(vo.getUri()).get(0);
            String description = vo.getDescription();
            String fileName =
                    Iterables.getFirst(StringUtils.splitLineByLine(description), uriFirstLine).replace('/', '-')
                            + ".md";
            String groupNames = endpoint.getGroupNames();
            File dir = DocAnalyzerConfig.getInstace().getDocOutputDirectoryPath()
                    .resolve(groupNames.replace('.', File.separatorChar)).toFile();
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    log.warn("mkdirs [{}] failed.", dir);
                    continue;
                }
            }
            File output = dir.toPath().resolve(fileName).toFile();
            try {
                FreeMarkerPrinter.printToFile(vo, output);
                if (alsoSendToShowdoc) {
                    ShowdocHttpInvoker.invoke(groupNames, description, FreeMarkerPrinter.printAsString(vo), sequence);
                    sequence++;
                }
            } catch (FreeMarkerPrintExcpetion e) {
                log.warn("FreeMarkerPrinter print failed.", e);
            }
        }

    }

    private String emptyToHorizontalLine(String linkName) {
        if (StringUtils.isEmpty(linkName)) {
            return horizontalLine;
        }
        return linkName;
    }

    private String convertValidators(Boolean required, Collection<PropertyValidatorDto> validators) {
        StringBuilder result = new StringBuilder(64);

        if (required) {
            result.append("必填");
        }

        if (validators != null && validators.size() > 0) {
            for (PropertyValidatorDto validator : validators) {
                result.append("　");
                result.append(validator.getValidatorType());
                result.append(validator.getNote());
            }
        }

        if (result.length() == 0) {
            return horizontalLine;
        } else {
            return result.toString();
        }
    }

}