package io.lighty.examples.controllers.restconf.ofp;

import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketInListener implements PacketProcessingListener {

    private static final Logger LOG = LoggerFactory.getLogger(PacketInListener.class);

    public PacketInListener() {
    }

    @Override
    public void onPacketReceived(PacketReceived notification) {
        LOG.trace("PacketIn Recived. {}", notification);
    }
}