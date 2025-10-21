/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.mountpoint.codecs;

/**
 * Codec which transforms two inputs to output.
 * @param <I1> type of input1
 * @param <I2> type of input2
 * @param <O> type of output
 */
public interface BiCodec<I1,I2,O> {

    O apply(I1 firstInput, I2 secondInput) throws GnmiCodecException;
}
