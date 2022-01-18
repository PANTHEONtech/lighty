/*
 * Copyright (c) 2022 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.examples.controllers.restapp;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev210321.HelloService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev210321.HelloWorldInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev210321.HelloWorldOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev210321.HelloWorldOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;


public final class HelloProvider implements HelloService {

    @Override
    public ListenableFuture<RpcResult<HelloWorldOutput>> helloWorld(final HelloWorldInput input) {
        final HelloWorldOutputBuilder helloBuilder = new HelloWorldOutputBuilder();
        //This is where you can call the external API
        //RestClient.POST(Payload)
        helloBuilder.setGreeting("Hello " + input.getName()); //Here you set output
        return RpcResultBuilder.success(helloBuilder.build()).buildFuture();
    }
}