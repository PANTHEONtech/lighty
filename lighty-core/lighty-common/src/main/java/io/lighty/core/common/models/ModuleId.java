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
import java.util.Objects;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;

/**
 * This class represents unique identifier of yang module.
 *
 * @author juraj.veverka
 */
public final class ModuleId {

    private final String nameSpace;
    private final String name;
    private final String revision;

    @JsonCreator
    public ModuleId(@JsonProperty("nameSpace") final String nameSpace, @JsonProperty("name") final String name,
            @JsonProperty("revision") final String revision) {
        this.nameSpace = nameSpace;
        this.name = name;
        this.revision = revision;
    }

    public String getName() {
        return this.name;
    }

    public String getRevision() {
        return this.revision;
    }

    public String getNameSpace() {
        return this.nameSpace;
    }

    public QName getQName() {
        return QName.create(this.nameSpace, this.revision, this.name);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModuleId)) {
            return false;
        }
        final ModuleId moduleId = (ModuleId) o;
        return Objects.equals(this.name, moduleId.name) && Objects.equals(this.revision, moduleId.revision) && Objects
                .equals(this.nameSpace, moduleId.nameSpace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.nameSpace, this.name, this.revision);
    }

    public static ModuleId from(final String nameSpace, final String name, final String revision) {
        return new ModuleId(nameSpace, name, revision);
    }

    public static ModuleId from(final YangModuleInfo yangModuleInfo) {
        final QName name = yangModuleInfo.getName();
        return new ModuleId(name.getNamespace().toString(), name.getLocalName(), name.getRevision().get().toString());
    }

    @Override
    public String toString() {
        return this.nameSpace + ":" + this.name + "@" + this.revision;
    }
}
