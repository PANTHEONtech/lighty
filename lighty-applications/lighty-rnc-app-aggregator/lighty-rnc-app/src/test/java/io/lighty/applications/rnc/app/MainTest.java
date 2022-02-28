/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.applications.rnc.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertThrows;

import com.beust.jcommander.ParameterException;
import io.lighty.applications.rnc.module.RncLightyModule;
import org.testng.annotations.Test;

public class MainTest {

    @Test
    public void testStartWithDefaultConfiguration() {
        Main app = spy(new Main());
        RncLightyModule lighty = mock(RncLightyModule.class);
        doReturn(true).when(lighty).initModules();
        doReturn(true).when(lighty).close();
        doReturn(lighty).when(app).createRncLightyModule(any(), eq(60));
        app.start(new String[] {});
    }

    @Test
    public void testStartWithConfigFile() {
        Main app = spy(new Main());
        RncLightyModule lighty = mock(RncLightyModule.class);
        doReturn(true).when(lighty).initModules();
        doReturn(true).when(lighty).close();
        doReturn(lighty).when(app).createRncLightyModule(any(), eq(90));
        app.start(new String[] {"-c","src/main/resources/configuration.json", "-t", "90"});
    }

    @Test
    public void testStartWithConfigFileNoSuchFile() {
        Main app = spy(new Main());
        app.start(new String[] {"-c","no_config.json"});
        verify(app, never()).createRncLightyModule(any(), any());
    }

    @Test
    public void testStartWithWrongTimeOut() {
        final Main app = spy(new Main());
        assertThrows(ParameterException.class, () -> app.start(new String[] {"-t", "WRONG_TIME_OUT"}));
        verify(app, never()).createRncLightyModule(any(), any());
    }
}
