/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.argparse4j.inf.ArgumentChoice;

public class GroupArguments {

    private final String groupName;
    private final String groupDescription;
    private final List<SingleOptionInGroup> arguments = new ArrayList<>();

    public GroupArguments(final String groupName, final String groupDescription) {
        this.groupDescription = groupDescription;
        this.groupName = groupName;
    }

    public void addOption(final String description, final List<String> name, final boolean action,
                          final String nargs, final Object defaultArg, final ArgumentChoice argumentChoice,
                          final Class<?> type) {
        arguments.add(new SingleOptionInGroup(description, name, action, nargs, defaultArg, argumentChoice, type));
    }

    public List<SingleOptionInGroup> getOptions() {
        return arguments;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getGroupDescription() {
        return groupDescription;
    }

    public static class SingleOptionInGroup {
        private final String description;
        private final List<String> name;
        private final boolean action;
        private final String nargs;
        private final Object defaultArg;
        private final ArgumentChoice choices;
        private final Class<?> type;

        public SingleOptionInGroup(final String description, final List<String> name, final boolean action,
                                   final String nargs, final Object defaultArg, final ArgumentChoice choices,
                                   final Class<?> type) {
            this.description = description;
            this.name = name;
            this.action = action;
            this.nargs = nargs;
            this.defaultArg = defaultArg;
            this.choices = choices;
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public List<String> getName() {
            return name;
        }

        public boolean isAction() {
            return action;
        }

        public String getNargs() {
            return nargs;
        }

        public Object getDefaultArg() {
            return defaultArg;
        }

        public ArgumentChoice getChoices() {
            return choices;
        }

        public Class<?> getType() {
            return type;
        }

    }
}
