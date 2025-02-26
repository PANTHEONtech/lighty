/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.commons.util;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;

/**
 * Immutable representation of Element-name together with optional Module-name.
 *
 * <p>This class may be helpful when preparing string values representing YANG elements, where representation of
 * `openconfig-interfaces:interfaces` can be parsed and manipulate withing objects of this class.</p>
 */
public class ElementNameWithModuleName {

    private final String elementName;
    private final String moduleName;

    public ElementNameWithModuleName(final String elementName, final String moduleName) {
        this.elementName = elementName;
        this.moduleName = moduleName;
    }

    public ElementNameWithModuleName(final String elementName) {
        this.elementName = elementName;
        this.moduleName = null;
    }

    public static ElementNameWithModuleName parseFromString(final String element) {
        final String[] elementWithModule = element.split(":", 2);
        if (elementWithModule.length > 1) {
            return new ElementNameWithModuleName(elementWithModule[1], elementWithModule[0]);
        } else {
            return new ElementNameWithModuleName(elementWithModule[0]);
        }
    }

    public String getElementName() {
        return elementName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public boolean hasModuleName() {
        return moduleName != null && !moduleName.isEmpty();
    }

    /**
     * Compare this element name with provided QName name and Module name (if this element has some module).
     *
     * @param quName QName to compare with this element name
     * @param module Module to compare with this element module-name
     * @return true only if the element has same name as provided QName and same module-name as provided Module name
     *     (if any module-name is present), otherwise false.
     */
    public boolean equals(final QName quName, final Module module) {
        if (quName.getLocalName().equals(this.elementName)) {
            // check module name also
            if (this.hasModuleName() && !module.getName().equals(this.moduleName)) {
                return false;
            }
            return true;
        }
        return false;
    }
}
