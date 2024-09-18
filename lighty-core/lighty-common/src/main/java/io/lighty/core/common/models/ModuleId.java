/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.common.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.XMLNamespace;

/**
 * This class represents unique identifier of yang module.
 *
 * @author juraj.veverka
 */
public final class ModuleId {

    // (Optional) Description to current element in JSON format e.g. "usedBy":"RESTCONF" or "usedBy":"NETCONF/RESTCONF"
    private final String usedBy;
    private final XMLNamespace nameSpace;
    private final String name;
    private final Revision revision;

    @JsonCreator
    public ModuleId(@JsonProperty("usedBy") final String usedBy, @JsonProperty("nameSpace") final String nameSpace,
            @JsonProperty("name") final String name, @JsonProperty("revision") final String revision) {
        this(usedBy, nameSpace, name, Revision.ofNullable(revision).orElse(null));
    }

    public ModuleId(final String nameSpace, final String name, final String revision) {
        this(nameSpace, name, Revision.ofNullable(revision).orElse(null));
    }

    public ModuleId(final String usedBy, final String nameSpace, final String name, final Revision revision) {
        this(usedBy, XMLNamespace.of(nameSpace), name, revision);
    }

    public ModuleId(final String nameSpace, final String name, final Revision revision) {
        this(null, XMLNamespace.of(nameSpace), name, revision);
    }

    public ModuleId(final String usedBy, final XMLNamespace nameSpace, final String name, final Revision revision) {
        this.usedBy = usedBy;
        this.nameSpace = nameSpace;
        this.name = name;
        this.revision = revision;
    }

    public String getName() {
        return this.name;
    }

    public Revision getRevision() {
        return this.revision;
    }

    public XMLNamespace getNameSpace() {
        return this.nameSpace;
    }

    public String getUsedBy() {
        return usedBy;
    }

    public QName getQName() {
        return QName.create(this.nameSpace, this.revision, this.name);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ModuleId)) {
            return false;
        }
        final ModuleId moduleId = (ModuleId) obj;
        return Objects.equals(this.name, moduleId.name) && Objects.equals(this.revision, moduleId.revision)
                && Objects.equals(this.nameSpace, moduleId.nameSpace);
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
        return new ModuleId(name.getNamespace().toString(), name.getLocalName(), name.getRevision().orElse(null));
    }

    @Override
    public String toString() {
        return this.nameSpace + ":" + this.name + "@" + (this.revision != null ? this.revision.toString() : "");
    }
}
