package com.spldeolin.allison1875.inspector.processor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import com.google.common.collect.Lists;
import com.spldeolin.allison1875.base.util.JsonUtils;
import com.spldeolin.allison1875.base.util.StringUtils;
import com.spldeolin.allison1875.inspector.InspectorConfig;
import com.spldeolin.allison1875.inspector.dto.PardonDto;
import lombok.experimental.Accessors;

/**
 * @author Deolin 2020-02-24
 */
@Accessors(fluent = true)
public class DetectPardonProc {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(DetectPardonProc.class);

    private final Collection<PardonDto> pardons = Lists.newArrayList();

    public DetectPardonProc process() {
        String pardonDirectoryPath = InspectorConfig.getInstance().getPardonDirectoryPath();
        if (!StringUtils.isEmpty(pardonDirectoryPath)) {
            Iterator<File> fileIterator = FileUtils
                    .iterateFiles(new File(pardonDirectoryPath), new String[]{"json"}, true);
            if (fileIterator.hasNext()) {
                File jsonFile = fileIterator.next();
                try {
                    String json = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
                    if (json.startsWith("[")) {
                        pardons.addAll(JsonUtils.toListOfObject(json, PardonDto.class));
                    } else {
                        pardons.add(JsonUtils.toObject(json, PardonDto.class));
                    }
                } catch (Exception e) {
                    log.error("Cannot load public ack from json file. [{}]", jsonFile, e);
                }
            }
        }
        return this;
    }

    public Collection<PardonDto> pardons() {
        return this.pardons;
    }

}
