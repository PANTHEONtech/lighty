/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the lighty.io-core
 * Fair License 5, version 0.9.1. You may obtain a copy of the License
 * at: https://github.com/PantheonTechnologies/lighty-core/LICENSE.md
 */
package io.lighty.core.controller.impl.actor;

import akka.actor.PoisonPill;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

public class ClusterEventActor extends UntypedActor {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterEventActor.class);
    public static final String CLUSTER_EVENT_ACTOR_NAME = "cluster-event-actor";

    private Cluster cluster;
    private CountDownLatch countDownLatch;
    private long poisonPillTimeoutMillis;

    public ClusterEventActor(CountDownLatch countDownLatch, long poisonPillTimeoutMillis) {
        this.countDownLatch = countDownLatch;
        this.poisonPillTimeoutMillis = poisonPillTimeoutMillis;
        this.cluster = Cluster.get(getContext().system());
    }

    @Override
    public void preStart() {
        LOG.debug("ClusterEventActor - preStart - subscribing");
        cluster.subscribe(getSelf(), ClusterEvent.MemberEvent.class);

        LOG.debug("Will send poison pill to self in {} milliseconds", poisonPillTimeoutMillis);
        getContext().system().scheduler().scheduleOnce(
                Duration.create(poisonPillTimeoutMillis, TimeUnit.MILLISECONDS),
                () -> getSelf().tell(PoisonPill.getInstance(), getSelf()),
                getContext().system().dispatcher());
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof ClusterEvent.CurrentClusterState) {
            ClusterEvent.CurrentClusterState clusterState = (ClusterEvent.CurrentClusterState) message;
            LOG.debug("ClusterEvent.CurrentClusterState: leader=" + clusterState.getLeader().toString());

            boolean allUp = true;
            for (Member member : clusterState.getMembers()) {
                if (!member.status().equals(akka.cluster.MemberStatus.up())) {
                    allUp = false;
                }
            }
            if (allUp && clusterState.leader().isDefined() && clusterState.leader().nonEmpty()) {
                countDownLatch.countDown();
            }
        }
    }

    @Override
    public void postStop() {
        LOG.debug("ClusterEventActor - postStop - unsubscribing");
        cluster.unsubscribe(getSelf());
        countDownLatch.countDown();
    }
}
