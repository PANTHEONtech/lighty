/*
 * Copyright (c) 2020 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa.service;

import io.lighty.core.controller.impl.config.ConfigurationException;
import java.util.Dictionary;

public interface ManagedService {
    void updated(Dictionary<String, ?> var1) throws ConfigurationException;
}
