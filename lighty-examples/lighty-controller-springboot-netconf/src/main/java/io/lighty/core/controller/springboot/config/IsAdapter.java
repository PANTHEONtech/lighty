/*
 * Copyright (c) 2018-2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.core.controller.springboot.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.jcasbin.persist.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IsAdapter implements Adapter {

    private static final Logger LOG = LoggerFactory.getLogger(IsAdapter.class);
    private static final String NOT_IMPLEMENTED = "not implemented";

    private final String classPath;

    public IsAdapter(String classPath) {
        this.classPath = classPath;
    }

    @Override
    public void loadPolicy(Model model) {
        if (classPath.equals("")) {
            // throw new Error("invalid file path, file path cannot be empty");
            return;
        }

        loadPolicyClassPath(model, Helper::loadPolicyLine);
    }

    @Override
    public void savePolicy(Model model) {
        LOG.warn("savePolicy is not implemented !");
    }

    @Override
    public void addPolicy(String sec, String ptype, List<String> rule) {
        throw new Error(NOT_IMPLEMENTED);
    }

    @Override
    public void removePolicy(String sec, String ptype, List<String> rule) {
        throw new Error(NOT_IMPLEMENTED);
    }

    @Override
    public void removeFilteredPolicy(String sec, String ptype, int fieldIndex, String... fieldValues) {
        throw new Error(NOT_IMPLEMENTED);
    }

    private void loadPolicyClassPath(Model model, Helper.loadPolicyLineHandler<String, Model> handler) {
        InputStream is = IsAdapter.class.getResourceAsStream(classPath);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

        String line;
        try {
            while((line = br.readLine()) != null) {
                handler.accept(line, model);
            }

            is.close();
            br.close();
        } catch (IOException e) {
            IllegalStateException ioErr = new IllegalStateException("IO error occurred", e);
            LOG.error("IO error occurred", ioErr);
            throw ioErr;
        }
    }

}
