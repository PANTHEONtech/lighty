/*
 * Copyright (c) 2021 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.yang.validator.checkupdatefrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CheckUpdateFromErrorRFC6020 {

    private static final String PRETEXT = "According to RFC 6020 ";
    private final String name;
    private final String description;
    private String newInformation;
    private String oldInformation;

    private static final Logger LOG = LoggerFactory.getLogger(CheckUpdateFromErrorRFC6020.class);

    static CheckUpdateFromErrorRFC6020 nameError() {
        return new CheckUpdateFromErrorRFC6020("name error",
                "name of the module MUST not change.");
    }

    static CheckUpdateFromErrorRFC6020 namespaceError() {
        return new CheckUpdateFromErrorRFC6020("namespace error",
                "namespace of the module MUST not change.");
    }

    static CheckUpdateFromErrorRFC6020 missingRevision() {
        return new CheckUpdateFromErrorRFC6020("missing revision",
                "revision on the new module MUST exist.");
    }

    static CheckUpdateFromErrorRFC6020 missingOldRevision() {
        return new CheckUpdateFromErrorRFC6020("missing old revision",
                "all the revisions from old module should be kept on the new module.");
    }

    static CheckUpdateFromErrorRFC6020 revisionError() {
        return new CheckUpdateFromErrorRFC6020("revision error",
                "revision on the new module MUST be greater then on the old module.");
    }

    static CheckUpdateFromErrorRFC6020 missingNodeError() {
        return new CheckUpdateFromErrorRFC6020("missing node error",
                "node from old module must be present in new module as well.");
    }

    static CheckUpdateFromErrorRFC6020 missingEnumError() {
        return new CheckUpdateFromErrorRFC6020("missing enum error",
                "an \"enumeration\" type may have new enums added, provided the old"
                        + " enums's values do not change.");
    }

    static CheckUpdateFromErrorRFC6020 missingBitError() {
        return new CheckUpdateFromErrorRFC6020("missing bit error",
                "bits may have new bits added but they can not be removed or its position changed.");
    }

    static CheckUpdateFromErrorRFC6020 patternError() {
        return new CheckUpdateFromErrorRFC6020("pattern error",
                "new patterns may be added but old ones can not be removed or changed.");
    }

    static CheckUpdateFromErrorRFC6020 lengthError() {
        return new CheckUpdateFromErrorRFC6020("length error",
                "length statement may expand the allowed value space.");
    }

    static CheckUpdateFromErrorRFC6020 rangeError() {
        return new CheckUpdateFromErrorRFC6020("range error",
                "range statement may only expand the allowed value space, it can not"
                        + " remove or shorten allowed value space.");
    }

    static CheckUpdateFromErrorRFC6020 defaultError() {
        return new CheckUpdateFromErrorRFC6020("default error",
                "a \"default\" statement may be added to a leaf that does not have a default value but "
                        + "can not be removed or changed.");
    }

    static CheckUpdateFromErrorRFC6020 unitsError() {
        return new CheckUpdateFromErrorRFC6020("units error",
                "a \"units\" statement may be added but can not be removed.");
    }

    static CheckUpdateFromErrorRFC6020 referenceError() {
        return new CheckUpdateFromErrorRFC6020("reference error",
                "a \"reference\" statement may be added or updated but can not be removed.");
    }

    static CheckUpdateFromErrorRFC6020 addedMustError() {
        return new CheckUpdateFromErrorRFC6020("added must error",
                "a \"must\" statement may be removed or its constraint relaxed, but new one can not be added.");
    }

    static CheckUpdateFromErrorRFC6020 checkMustWarning() {
        return new CheckUpdateFromErrorRFC6020("check must warning",
                "a \"must\" statement may be removed or its constraint relaxed. \"Must\" exists and might have"
                        + " been relaxed but should be check by user if the constraint isn t changed or more strict.");
    }

    static CheckUpdateFromErrorRFC6020 mandatoryError() {
        return new CheckUpdateFromErrorRFC6020("mandatory error",
                "a \"mandatory\" statement may be removed or changed from \"true\" to \"false\", "
                        + "but it can not become mandatory if it was not.");
    }

    static CheckUpdateFromErrorRFC6020 illegalConfigStateError() {
        return new CheckUpdateFromErrorRFC6020("illegal config state error",
                "a node that represented state data may be changed to represent configuration,"
                        + " provided it is not mandatory, but can change to configuration if it is not mandatory");
    }

    static CheckUpdateFromErrorRFC6020 illegalConfigChangeError() {
        return new CheckUpdateFromErrorRFC6020("illegal config change error",
                "a node that represented configuration data may not be changed to represent state data");
    }

    static CheckUpdateFromErrorRFC6020 statusError() {
        return new CheckUpdateFromErrorRFC6020("status error",
                "a \"status\" statement may be added, or changed from \"current\""
                        + " to \"deprecated\" or \"obsolete\", or from \"deprecated\" to \"obsolete\","
                        + " but not other way arround");
    }

    static CheckUpdateFromErrorRFC6020 typeError() {
        return new CheckUpdateFromErrorRFC6020("type error",
                "a \"type\" statement may not be replaced with another type statement that changes"
                        + " syntax or semantics of the type");
    }

    static CheckUpdateFromErrorRFC6020 minElementsError() {
        return new CheckUpdateFromErrorRFC6020("min-elements error",
                "a \"min-elements\" statement may be removed, or changed to require"
                        + " fewer elements, it can not require more elements or be added");
    }

    static CheckUpdateFromErrorRFC6020 maxElementsError() {
        return new CheckUpdateFromErrorRFC6020("max-elements error",
                "A \"max-elements\" statement may be removed, or changed to allow"
                        + " more elements, it can not require less elements");
    }

    protected CheckUpdateFromErrorRFC6020(final String name, final String description) {
        this.name = name;
        this.description = description;
        this.newInformation = "";
        this.oldInformation = "";
    }

    CheckUpdateFromErrorRFC6020 updateInformation(final String newInfo, final String oldInfo) {
        this.newInformation = " New module-> " + newInfo;
        this.oldInformation = " | Old module-> " + oldInfo;
        return this;
    }

    void print(int order) {
        LOG.error("{} {}: {}{}{}{}\n", order, name, PRETEXT, description, newInformation, oldInformation);
    }
}
