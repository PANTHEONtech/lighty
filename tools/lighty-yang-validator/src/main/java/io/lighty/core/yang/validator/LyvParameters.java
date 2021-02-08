/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator;

import static net.sourceforge.argparse4j.impl.Arguments.storeTrue;
import static net.sourceforge.argparse4j.impl.Arguments.version;

import com.google.common.base.Preconditions;
import io.lighty.core.yang.validator.checkupdatefrom.CheckUpdateFrom;
import io.lighty.core.yang.validator.formats.Format;
import java.io.File;
import java.util.Collections;
import java.util.List;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

public class LyvParameters {

    private final ArgumentParser lyvArgumentParser = ArgumentParsers.newFor("LYV").build()
            .defaultHelp(true)
            .version("Version: ${prog} 13.1.1-SNAPSHOT\nContact: sales@pantheon.tech")
            .description("Yangtools based yang module parser");
    private final Format formatter;
    private final String[] args;

    public LyvParameters(final Format formatter, final String[] args) {
        this.formatter = formatter;
        this.args = args;
    }

    public Namespace parseArguments() {
        lyvArgumentParser.addArgument("-m", "--module-name")
                .nargs("*")
                .help("validate yang model by module name.");
        lyvArgumentParser.addArgument("-e", "--features")
                .nargs("*")
                .help("feature is a string in the form [($namespace?revision=$revision)$local_name]."
                        + " This option is used to prune the data model by removing all nodes that are "
                        + "defined with a \"if-feature\".");
        lyvArgumentParser.addArgument("-o", "--output")
                .nargs("?")
                .help("path to output directory for logs. Output dir will be created with name out.log"
                        + " if this file already exists it will be overwritten");
        lyvArgumentParser.addArgument("-d", "--debug")
                .action(storeTrue())
                .help("add debug output");
        lyvArgumentParser.addArgument("-v", "--version")
                .action(version())
                .help("output release version and contact.");
        lyvArgumentParser.addArgument("-q", "--quiet")
                .action(storeTrue())
                .help("completely suppress output.");
        lyvArgumentParser.addArgument("-r", "--recursive")
                .action(storeTrue())
                .help("recursive search of directories specified by -p option.");
        lyvArgumentParser.addArgument("-p", "--path")
                .nargs("*")
                .setDefault(Collections.emptyList())
                .help("path is a colon (:) separated list of directories to search for yang modules.");
        lyvArgumentParser.addArgument("--check-update-from")
                .nargs("?")
                .help("path is a colon (:) separated list of directories to search for yang modules.");
        lyvArgumentParser.addArgument("-a", "--parse-all")
                .nargs("*")
                .setDefault(Collections.emptyList())
                .help("Parse all files within given directory/directories. This option can be used with -p option.");
        lyvArgumentParser.addArgument("-s", "--simplify")
                .nargs("?")
                .help("Simplify yang modules providing directory to xml files."
                        + " Based on these xml files it will generate simplified yang modules."
                        + "Use with -o or --output to save yang files"
                        + " to specified directory otherwise they will be printed out to standard output");
        lyvArgumentParser.addArgument("yang").nargs("*")
                .help("Yang models to be parsed");

        formatter.createOptions(this);
        addGroupArguments(CheckUpdateFrom.getGroupArguments());

        return validate(args);
    }

    private Namespace validate(final String[] argsToValidate) {
        final Namespace namespace = lyvArgumentParser.parseArgsOrFail(argsToValidate);
        final String pathDoesNotExist = "Path %s does not exist";
        if (namespace.getList("parse_all").isEmpty()) {
            final List<String> yangModules = namespace.getList("yang");
            final String errorMessage = "Did you forget to set the module to parse?";
            Preconditions.checkNotNull(yangModules, errorMessage);
            Preconditions.checkArgument(!yangModules.isEmpty(), errorMessage);
            final String pathIsNotFile = "Path %s is not a file";
            for (final String yang : yangModules) {
                File file = new File(yang);
                Preconditions.checkArgument(file.exists(), String.format(pathDoesNotExist, yang));
                Preconditions.checkArgument(file.isFile(), String.format(pathIsNotFile, yang));
            }
            final String checkUpdateFrom = namespace.getString("check_update_from");
            if (checkUpdateFrom != null) {
                File file = new File(checkUpdateFrom);
                Preconditions.checkArgument(file.exists(), String.format(pathDoesNotExist, checkUpdateFrom));
                Preconditions.checkArgument(file.isFile(), String.format(pathIsNotFile, checkUpdateFrom));
                final List<String> paths = namespace.getList("check_update_from_path");
                for (final String path : paths) {
                    file = new File(path);
                    Preconditions.checkArgument(file.exists(), String.format(pathDoesNotExist, path));
                    Preconditions.checkArgument(file.isDirectory(), String.format("Path %s is not a directory", path));
                }
            }
        }
        final List<String> paths = namespace.getList("path");
        for (final String path : paths) {
            File file = new File(path);
            Preconditions.checkArgument(file.exists(), String.format(pathDoesNotExist, path));
            Preconditions.checkArgument(file.isDirectory(), String.format("Path %s is not a directory", path));
        }

        return namespace;
    }

    public void addFormatArgument(String formats) {
        lyvArgumentParser.addArgument("-f", "--format")
                .nargs("?")
                .help("output format of the yang. Supported formats: " + formats);
    }

    public void addGroupArguments(final GroupArguments groupArguments) {
        final List<GroupArguments.SingleOptionInGroup> options = groupArguments.getOptions();
        final ArgumentGroup argGroup = lyvArgumentParser.addArgumentGroup(groupArguments.getGroupName())
                .description(groupArguments.getGroupDescription());
        for (GroupArguments.SingleOptionInGroup option : options) {
            Argument arg;
            if (option.getName().size() == 1) {
                arg = argGroup.addArgument(option.getName().get(0))
                        .type(option.getType())
                        .help(option.getDescription());
            } else {
                arg = argGroup.addArgument(option.getName().get(0), option.getName().get(1))
                        .type(option.getType())
                        .help(option.getDescription());
            }
            if (option.isAction()) {
                arg.action(storeTrue());
            } else {
                arg.nargs(option.getNargs());
            }
            arg.choices(option.getChoices());
            if (option.getDefaultArg() != null) {
                arg.setDefault(option.getDefaultArg());
            }
        }
    }
}
