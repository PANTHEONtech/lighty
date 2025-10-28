/*
 * Copyright (c) 2018 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.aaa.config;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.ShiroConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.ShiroConfigurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.shiro.ini.Main;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.shiro.ini.MainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.shiro.ini.Urls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.aaa.app.config.rev170619.shiro.ini.UrlsBuilder;

public final class ShiroConfigurationConfig {

    private static final String ROLES_ADMIN = "authcBasic, roles[admin]";

    private ShiroConfigurationConfig() {

    }

    public static ShiroConfiguration getDefault() {
        final List<Main> mains = new ArrayList<>();
        mains.add(initMain("tokenAuthRealm", "org.opendaylight.aaa.shiro.realm.TokenAuthRealm"));
        mains.add(initMain("securityManager.realms", "$tokenAuthRealm"));
        mains.add(initMain("accountingListener", "org.opendaylight.aaa.shiro.filters.AuthenticationListener"));
        mains.add(initMain("securityManager.authenticator.authenticationListeners", "$accountingListener"));
        mains.add(initMain("dynamicAuthorization", "org.opendaylight.aaa.shiro.realm.MDSALDynamicAuthorizationFilter"));

        final List<Urls> urls = new ArrayList<>();
        urls.add(initUrl("/operations/cluster-admin**", ROLES_ADMIN));
        urls.add(initUrl("/v1/**", ROLES_ADMIN));
        urls.add(initUrl("/config/aaa*/**", ROLES_ADMIN));
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

