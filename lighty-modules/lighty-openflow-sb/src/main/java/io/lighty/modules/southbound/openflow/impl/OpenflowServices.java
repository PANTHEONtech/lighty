package io.lighty.modules.southbound.openflow.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflowplugin.app.frm.reconciliation.service.rev180227.FrmReconciliationService;

public interface OpenflowServices {

    FrmReconciliationService getFrmReconciliationService();
}