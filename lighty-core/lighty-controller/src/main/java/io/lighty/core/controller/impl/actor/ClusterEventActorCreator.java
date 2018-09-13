/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
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
