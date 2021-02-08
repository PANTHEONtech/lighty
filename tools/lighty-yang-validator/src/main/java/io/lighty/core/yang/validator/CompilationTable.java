/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator;

import com.google.common.io.Resources;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CompilationTable {

    private static final String TD = "<td>";
    private static final String TD_END = "</td>";
    private static final Logger LOG = LoggerFactory.getLogger(CompilationTable.class);

    private final StringBuilder tableRowBuilder;
    private final String outputDir;
    private final String yangDirectory;
    private final String yangtoolsVersion;
    private final Map<String, YangResult> rows = new HashMap<>();
    private File htmlFile = null;

    private int numOfFailed;
    private int numOfPassed;
    private int numOfWarn;

    CompilationTable(@Nullable final String outputDir, final List<String> yangDirectory,
                     final String yangtoolsVersion) {
        tableRowBuilder = new StringBuilder();
        if (outputDir == null) {
            this.outputDir = System.getProperty("java.io.tmpdir");
        } else {
            this.outputDir = outputDir;
        }
        this.yangDirectory = String.join(",", yangDirectory);
        this.yangtoolsVersion = yangtoolsVersion;
    }

    void addRow(final String name, @Nullable final String result, final CompilationStatus status) {
        if (htmlFile == null) {
            if (rows.containsKey(name)) {
                final YangResult yangResult = rows.get(name);
                yangResult.update(result, status);
            } else {
                switch (status) {
                    case FAILED:
                        numOfFailed++;
                        break;
                    case PASSED:
                        numOfPassed++;
                        break;
                    case PASSED_WITH_WARNINGS:
                        numOfWarn++;
                        break;
                    default:
                        break;
                }
                rows.put(name, new YangResult(result, status));
            }
        } else {
            LOG.warn("Can not add another row. Html already created");
        }
    }

    void buildHtml() {
        if (htmlFile == null) {
            for (final Map.Entry<String, YangResult> entries : rows.entrySet()) {
                final YangResult value = entries.getValue();
                tableRowBuilder.append("<tr>")
                        .append(TD)
                        .append(entries.getKey())
                        .append(TD_END)
                        .append(TD)
                        .append(value.getStatus())
                        .append(TD_END)
                        .append(TD)
                        .append(value.getResult())
                        .append(TD_END)
                        .append("</tr>");
            }

            tableRowBuilder.append("</tbody>")
                    .append("</table>")
                    .append("</div>")
                    .append("</body>")
                    .append("</html>");
        } else {
            LOG.warn("Can not wrap html again. Html already created");
        }
        build();
    }

    private void build() {
        if (htmlFile == null) {
            URL url = Resources.getResource("table");
            String text;
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd@HH:mm:ss");
            Date date = new Date(System.currentTimeMillis());
            String outputFile = outputDir + "/" + "compilation_results" + formatter.format(date) + ".html";
            try (FileWriter writer = new FileWriter(outputFile)) {
                text = Resources.toString(url, StandardCharsets.UTF_8);
                text = text.replace("<DIRECTORY>", yangDirectory);
                text = text.replace("<YANGTOOLS_VERSION>", yangtoolsVersion);
                text = text.replace("<BODY>", tableRowBuilder.toString());
                text = text.replace("<PASSED_COMP>", Integer.toString(numOfPassed));
                text = text.replace("<ERROR_COMP>", Integer.toString(numOfFailed));
                text = text.replace("<WARN_COMP>", Integer.toString(numOfWarn));
                writer.write(text);
            } catch (IOException e) {
                LOG.error("Can not load text from table file");
            }
            LOG.info("html generated to {}", outputFile);
            htmlFile = Paths.get(outputFile).toFile();
        }
    }

    private static class YangResult {

        private CompilationStatus status;
        private StringBuilder result = new StringBuilder();

        YangResult(String result, final CompilationStatus status) {
            if (result == null) {
                result = "";
            }
            this.result.append(result);
            this.status = status;
        }

        void update(String updatedResult, final CompilationStatus updatedStatus) {
            if (updatedStatus.equals(CompilationStatus.FAILED)) {
                this.status = CompilationStatus.FAILED;
            }
            if (updatedResult == null) {
                updatedResult = "";
            }
            this.result.append("\n")
                    .append(updatedResult);
        }

        String getStatus() {
            return this.status.toString();
        }

        String getResult() {
            return this.result.toString();
        }
    }
}
