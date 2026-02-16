/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa.config;

import java.util.List;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.ShiroConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.shiro.ini.Main;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.shiro.ini.Urls;
import org.testng.Assert;
import org.testng.annotations.Test;

class ShiroConfigurationConfigTest {

    @Test
    void getDefaultTest() {
        ShiroConfiguration configuration = ShiroConfigurationConfig.getDefault();

        @Nullable List<Main> mains = configuration.getMain();
        @Nullable List<Urls> urls = configuration.getUrls();

        Assert.assertNotNull(mains);
        Assert.assertTrue(containsMainKey("tokenAuthRealm", mains));
        Assert.assertTrue(containsMainKey("securityManager.realms", mains));
        Assert.assertTrue(containsMainKey("accountingListener", mains));
        Assert.assertTrue(containsMainKey("securityManager.authenticator.authenticationListeners", mains));
        Assert.assertTrue(containsMainKey("dynamicAuthorization", mains));

        Assert.assertNotNull(urls);
        Assert.assertTrue(containsUrlKey("/operations/cluster-admin**", urls));
        Assert.assertTrue(containsUrlKey("/v1/**", urls));
        Assert.assertTrue(containsUrlKey("/config/aaa*/**", urls));
        Assert.assertTrue(containsUrlKey("/**", urls));
    }

    private boolean containsMainKey(String key, List<Main> mains) {
        return mains.stream().anyMatch(main -> main.key().getPairKey().equals(key));
    }

    private boolean containsUrlKey(String key, List<Urls> urls) {
        return urls.stream().anyMatch(url -> url.key().getPairKey().equals(key));
    }
}
