package com.spldeolin.allison1875.si.processor;

import static com.spldeolin.allison1875.si.StatuteInspectorConfig.CONFIG;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import com.spldeolin.allison1875.base.util.Csvs;
import com.spldeolin.allison1875.si.vo.LawlessVo;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

/**
 * @author Deolin 2020-02-22
 */
@Log4j2
@Accessors(fluent = true)
public class LawlessReportProcessor {

    @Setter
    private Collection<LawlessVo> lawlesses;

    public void report() {
        String csvContent = Csvs.writeCsv(lawlesses, LawlessVo.class);
        try {
            FileUtils.writeStringToFile(CONFIG.getLawlessReportCsvPath().toFile(), csvContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error(e);
        }
    }

}