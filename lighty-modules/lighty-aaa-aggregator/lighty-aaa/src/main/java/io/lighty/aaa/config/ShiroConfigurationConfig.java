/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa.config;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.ShiroConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.ShiroConfigurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.shiro.configuration.Main;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.shiro.configuration.MainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.shiro.configuration.Urls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.shiro.configuration.UrlsBuilder;

public final class ShiroConfigurationConfig {
    private ShiroConfigurationConfig() {

    }

    public static ShiroConfiguration getDefault() {
        final List<Main> mains = new ArrayList<>();
        mains.add(initMain("tokenAuthRealm", "org.opendaylight.aaa.shiro.realm.TokenAuthRealm"));
        mains.add(initMain("securityManager.realms", "$tokenAuthRealm"));
        mains.add(initMain("authcBasic", "org.opendaylight.aaa.shiro.filters.ODLHttpAuthenticationFilter"));
        mains.add(initMain("accountingListener", "org.opendaylight.aaa.shiro.filters.AuthenticationListener"));
        mains.add(initMain("securityManager.authenticator.authenticationListeners", "$accountingListener"));
        mains.add(initMain("dynamicAuthorization", "org.opendaylight.aaa.shiro.realm.MDSALDynamicAuthorizationFilter"));

        final List<Urls> urls = new ArrayList<>();
        urls.add(initUrl("/operations/cluster-admin**", "authcBasic, roles[admin]"));
        urls.add(initUrl("/v1/**", "authcBasic, roles[admin]"));
        urls.add(initUrl("/config/aaa*/**", "authcBasic, roles[admin]"));
        urls.add(initUrl("/**", "authcBasic"));

        return new ShiroConfigurationBuilder().setMain(mains).setUrls(urls).build();
    }

    private static Urls initUrl(final String key, final String val) {
        return new UrlsBuilder().setPairKey(key).setPairValue(val).build();
    }

    private static Main initMain(final String key, final String val) {
        return new MainBuilder().setPairKey(key).setPairValue(val).build();
    }
}

