/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.common.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;

/**
 * This class represents unique identifier of yang module.
 *
 * @author juraj.veverka
 */
public final class ModuleId {

    private final QName qname;

    @JsonCreator
    public ModuleId(@JsonProperty("nameSpace") final String nameSpace,
                    @JsonProperty("name") final String name,
                    @JsonProperty("revision") final String revision) {
        this(QName.create(QNameModule.create(URI.create(nameSpace), Revision.ofNullable(revision)), name));
    }

    public ModuleId(final QName qname) {
        this.qname = Objects.requireNonNull(qname);
    }

    public QName getQName() {
        return qname;
    }

    public String getName() {
        return qname.getLocalName();
    }

    public String getRevision() {
        return qname.getModule().getRevision().map(Revision::toString).orElse(null);
    }

    public String getNameSpace() {
        return qname.getModule().getNamespace().toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ModuleId && qname.equals(((ModuleId) o).qname);
    }

    @Override
    public int hashCode() {
        return qname.hashCode();
    }

    public static ModuleId from(final String nameSpace, final String name, final String revision) {
        return new ModuleId(nameSpace, name, revision);
    }

    public static ModuleId from(final YangModuleInfo yangModuleInfo) {
        return new ModuleId(yangModuleInfo.getName());
    }

    @Override
    public String toString() {
        return getNameSpace() + ":" + getName() + "@" + getRevision();
    }
}
