/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.capabilities;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.concepts.SemVer;
import org.opendaylight.yangtools.yang.common.Revision;

// Serializable because spotbugs report in SchemaException which extends Exception which is serializable
public class GnmiDeviceCapability implements Serializable {
    private static final long serialVersionUID = 1;

    private final String name;
    private final SemVer semanticVersion;
    private final Revision revision;

    public GnmiDeviceCapability(final String name, @Nullable final SemVer semVer, @Nullable final Revision revision) {
        this.name = name;
        this.semanticVersion = semVer;
        this.revision = revision;
    }

    public GnmiDeviceCapability(final String name, @Nullable final String semver, @Nullable final String revision) {
        this.name = name;
        this.semanticVersion = semver == null ? null : SemVer.valueOf(semver);
        this.revision = revision == null ? null : Revision.of(revision);

    }

    public GnmiDeviceCapability(final String name) {
        this.name = name;
        this.revision = null;
        this.semanticVersion = null;
    }

    public String getName() {
        return name;
    }

    public Optional<SemVer> getSemVer() {
        return Optional.ofNullable(semanticVersion);
    }

    public Optional<Revision> getRevision() {
        return Optional.ofNullable(revision);
    }


    public Optional<String> getVersionString() {
        if (semanticVersion != null) {
            return Optional.of(semanticVersion.toString());
        } else if (revision != null) {
            return Optional.of(revision.toString());
        }
        return Optional.empty();
    }

    public String toString() {
        return name
                + (getSemVer().map(semVer -> " semver: " + semVer).orElse(""))
                + (getRevision().map(rev -> " revision: " + rev).orElse(""));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GnmiDeviceCapability)) {
            return false;
        }
        GnmiDeviceCapability other = (GnmiDeviceCapability) obj;
        return Objects.equals(this.name, other.name)
                && Objects.equals(this.semanticVersion, other.semanticVersion)
                && Objects.equals(this.revision, other.revision);
    }

    public int hashCode() {
        return Objects.hash(name, semanticVersion, revision);
    }
}
