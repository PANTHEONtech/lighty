/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.impl.actor;

import akka.japi.Creator;

import java.util.concurrent.CountDownLatch;

/**
 * Created by Samuel on 22.9.2017.
 */
public class ClusterEventActorCreator implements Creator<ClusterEventActor> {

    private CountDownLatch countDownLatch;
    private long poisonPillTimeoutMillis;

    public ClusterEventActorCreator(CountDownLatch countDownLatch, long poisonPillTimeoutMillis) {
        this.countDownLatch = countDownLatch;
        this.poisonPillTimeoutMillis = poisonPillTimeoutMillis;
    }

    @Override
    public ClusterEventActor create() throws Exception {
        return new ClusterEventActor(countDownLatch, poisonPillTimeoutMillis);
    }
}
