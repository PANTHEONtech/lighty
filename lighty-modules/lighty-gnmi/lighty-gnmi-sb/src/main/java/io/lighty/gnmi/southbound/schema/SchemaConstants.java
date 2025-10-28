/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.schema;

import org.opendaylight.yangtools.yang.parser.rfc7950.reactor.RFC7950Reactors;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;

public final class SchemaConstants {

    private SchemaConstants() {
        // Utility class
    }

    public static final String SEMVER_REGEX = "\\d+[.]\\d+[.]\\d+[mM]?(-[\\w\\d.]+)?([+][\\w\\d\\.]+)?";

    public static final String REVISION_REGEX = "\\d{4}-\\d{2}-\\d{2}";

    /**
     * Default reactor used for parsing yang modules with openconfig semver support enabled.
     */
    public static final CrossSourceStatementReactor DEFAULT_YANG_REACTOR = RFC7950Reactors.defaultReactorBuilder()
            .build();
}
