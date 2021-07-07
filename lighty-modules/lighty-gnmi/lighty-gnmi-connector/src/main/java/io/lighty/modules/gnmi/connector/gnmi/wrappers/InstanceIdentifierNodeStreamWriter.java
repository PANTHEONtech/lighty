/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.gnmi.connector.gnmi.wrappers;

import java.io.IOException;
import javax.xml.transform.dom.DOMSource;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceIdentifierNodeStreamWriter implements NormalizedNodeStreamWriter {

    private final StringBuilder urlBuilder;
    private static final Logger LOG = LoggerFactory.getLogger(InstanceIdentifierNodeStreamWriter.class);
    private static final String NOT_IMPLEMENTED = "Method not implemented";

    public InstanceIdentifierNodeStreamWriter(String prefix) {
        this.urlBuilder = new StringBuilder(prefix + ":");
    }

    /*
    *Not implemented
     */
    @Override
    public void startLeafNode(YangInstanceIdentifier.NodeIdentifier name) throws IOException {
        LOG.warn(NOT_IMPLEMENTED);
    }

    /*
     *Not implemented
     */
    @Override
    public void startLeafSet(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException {
        LOG.warn(NOT_IMPLEMENTED);
    }

    /*
     *Not implemented
     */
    @Override
    public void startOrderedLeafSet(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException {
        LOG.warn(NOT_IMPLEMENTED);
    }

    /*
     *Not implemented
     */
    @Override
    public void startLeafSetEntryNode(YangInstanceIdentifier.NodeWithValue<?> name) throws IOException {
        LOG.warn(NOT_IMPLEMENTED);
    }

    @Override
    public void startContainerNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException {
        this.urlBuilder.append(name.getNodeType().getLocalName() + "/");
    }

    /*
     *Not implemented
     */
    @Override
    public void startUnkeyedList(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException {
        LOG.warn(NOT_IMPLEMENTED);
    }

    /*
     *Not implemented
     */
    @Override
    public void startUnkeyedListItem(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException {
        LOG.warn(NOT_IMPLEMENTED);
    }

    @Override
    public void startMapNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException {
        this.urlBuilder.append(name.getNodeType().getLocalName() + "/");
    }

    @Override
    public void startMapEntryNode(YangInstanceIdentifier.NodeIdentifierWithPredicates identifier, int childSizeHint)
        throws IOException {
        this.urlBuilder.append("[");
        for (var key:identifier.keySet()) {
            @Nullable Object tmp = identifier.getValue(key);
            if (tmp instanceof QName) {
                this.urlBuilder.append(key.getLocalName() + "=" + ((QName) tmp).getLocalName() + ";");
            } else {
                this.urlBuilder.append(key.getLocalName() + "=" + identifier.getValue(key) + ";");
            }
        }
        this.urlBuilder.append("]");
    }

    /*
     *Not implemented
     */
    @Override
    public void startOrderedMapNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException {
        LOG.warn(NOT_IMPLEMENTED);
    }

    /*
     *Not implemented
     */
    @Override
    public void startChoiceNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException {
        LOG.warn(NOT_IMPLEMENTED);
    }

    /*
     *Not implemented
     */
    @Override
    public void startAugmentationNode(YangInstanceIdentifier.AugmentationIdentifier identifier)
        throws IOException {
        LOG.warn(NOT_IMPLEMENTED);
    }

    /*
     *Not implemented
     */
    @Override
    public boolean startAnydataNode(YangInstanceIdentifier.NodeIdentifier name, Class<?> objectModel)
        throws IOException {
        return false;
    }

    /*
     *Not implemented
     */
    @Override
    public boolean startAnyxmlNode(YangInstanceIdentifier.NodeIdentifier name, Class<?> objectModel)
        throws IOException {
        return false;
    }

    /*
     *Not implemented
     */
    @Override
    public void domSourceValue(DOMSource value) throws IOException {
        LOG.warn(NOT_IMPLEMENTED);
    }

    /*
     *Not implemented
     */
    @Override
    public void startYangModeledAnyXmlNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint)
        throws IOException {
        LOG.warn(NOT_IMPLEMENTED);
    }

    /*
     *Not implemented
     */
    @Override
    public void endNode() throws IOException {
        LOG.warn(NOT_IMPLEMENTED);
    }

    /*
     *Not implemented
     */
    @Override
    public void scalarValue(@NonNull Object value) throws IOException {
        LOG.warn(NOT_IMPLEMENTED);
    }

    /*
     *Not implemented
     */
    @Override
    public void close() throws IOException {
        LOG.warn(NOT_IMPLEMENTED);
    }

    /*
     *Not implemented
     */
    @Override
    public void flush() throws IOException {
        LOG.warn(NOT_IMPLEMENTED);
    }

    public String getUrl() {
        String result = this.urlBuilder.toString();
        result = result.replace("/[", "[");
        result = result.replace(";]", "]");
        return result;
    }
}
