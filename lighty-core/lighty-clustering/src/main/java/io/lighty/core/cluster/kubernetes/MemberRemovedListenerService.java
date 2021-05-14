/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.core.cluster.kubernetes;

import akka.Done;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.cluster.Cluster;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.cluster.admin.rev151013.ClusterAdminService;

public class MemberRemovedListenerService implements ClusterSingletonService {
    private static final String CLUSTER_SINGLETON_ID = "singletonMemberRemovedListener";
    private static final String ACTOR_NAME = "memberRemovedListener";

    private final Cluster cluster;
    private final ActorSystem actorSystem;
    private final DataBroker dataBroker;
    private final ClusterAdminService clusterAdminRPCService;
    private ActorRef memberRemovedListener;

    public MemberRemovedListenerService(ActorSystem actorSystem, DataBroker dataBroker,
            ClusterAdminService clusterAdminRPCService) {
        this.actorSystem = actorSystem;
        this.dataBroker = dataBroker;
        this.clusterAdminRPCService = clusterAdminRPCService;
        this.cluster = Cluster.get(actorSystem);
    }

    @Override
    public void instantiateServiceInstance() {
        this.memberRemovedListener = actorSystem.actorOf(MemberRemovedListener.props(dataBroker,
                clusterAdminRPCService), ACTOR_NAME);
    }

    @Override
    public ListenableFuture<? extends Object> closeServiceInstance() {
        cluster.unsubscribe(this.memberRemovedListener);
        this.memberRemovedListener.tell(PoisonPill.getInstance(), this.memberRemovedListener);
        return Futures.immediateFuture(Done.done());
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return ServiceGroupIdentifier.create(CLUSTER_SINGLETON_ID);
    }
}
