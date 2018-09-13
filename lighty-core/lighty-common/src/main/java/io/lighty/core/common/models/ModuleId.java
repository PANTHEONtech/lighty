/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.common.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

import java.util.Objects;

/**
 * This class represents unique identifier of yang module.
 *
 * @author juraj.veverka
 */
public final class ModuleId {

    private String nameSpace;
    private String name;
    private String revision;

    @JsonCreator
    public ModuleId(@JsonProperty("nameSpace") String nameSpace,
                    @JsonProperty("name") String name,
                    @JsonProperty("revision") String revision) {
        this.nameSpace = nameSpace;
        this.name = name;
        this.revision = revision;
    }

    public String getName() {
        return name;
    }

    public String getRevision() {
        return revision;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModuleId)) return false;
        ModuleId moduleId = (ModuleId) o;
        return Objects.equals(name, moduleId.name) &&
                Objects.equals(revision, moduleId.revision) &&
                Objects.equals(nameSpace, moduleId.nameSpace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nameSpace, name, revision);
    }

    public static ModuleId from(String nameSpace, String name, String revision) {
        return new ModuleId(nameSpace, name, revision);
    }

    public static ModuleId from(YangModuleInfo yangModuleInfo) {
        return new ModuleId(yangModuleInfo.getNamespace(), yangModuleInfo.getName(), yangModuleInfo.getRevision());
    }

    @Override
    public String toString() {
        return nameSpace + ":" + name + "@" + revision;
    }

}
