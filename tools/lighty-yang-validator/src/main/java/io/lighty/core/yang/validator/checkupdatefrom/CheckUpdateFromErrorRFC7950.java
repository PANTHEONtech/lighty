/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.checkupdatefrom;

final class CheckUpdateFromErrorRFC7950 extends CheckUpdateFromErrorRFC6020 {

    static CheckUpdateFromErrorRFC7950 addedWhenError() {
        return new CheckUpdateFromErrorRFC7950("added when error",
                "a \"when\" statement may be removed or its constraint relaxed, but new one can not be added.");
    }

    static CheckUpdateFromErrorRFC7950 checkWhenWarning() {
        return new CheckUpdateFromErrorRFC7950("check when warning",
                "a \"when\" statement may be removed or its constraint relaxed. \"When\" exists and might have been"
                        + " relaxed but should be check by user if the constraint isn t changed or more strict.");
    }

    static CheckUpdateFromErrorRFC7950 missingBaseIdentityError() {
        return new CheckUpdateFromErrorRFC7950("missing base identity error",
                "a \"base\" statement may be added to an \"identity\" statement but it can not be changed"
                        + " with other \"base\" statement.");
    }

    static CheckUpdateFromErrorRFC7950 baseIdentityError() {
        return new CheckUpdateFromErrorRFC7950("base identity error",
                "a \"base\" statement may be added to an \"identity\" statement but it can not be removed.");
    }

    static CheckUpdateFromErrorRFC7950 missingIdentityError() {
        return new CheckUpdateFromErrorRFC7950("missing identity error",
                "an identity should not be removed.");
    }

    static CheckUpdateFromErrorRFC7950 identityRefBaseError() {
        return new CheckUpdateFromErrorRFC7950("identityRef base error",
                "A \"base\" statement may be removed from an \"identityref\" type "
                        + "provided there is at least one \"base\" statement left.");
    }

    private CheckUpdateFromErrorRFC7950(String name, String description) {
        super(name, description);
    }
}
