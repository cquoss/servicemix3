/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.jbi.messaging;

import java.io.IOException;
import java.io.ObjectInput;

import javax.jbi.messaging.InOptionalOut;

/**
 * InOptionalOut message exchange.
 * 
 * @version $Revision$
 */
public class InOptionalOutImpl extends MessageExchangeImpl implements InOptionalOut {

    private static final long serialVersionUID = -3145649037372074912L;

    private static final int[][] STATES_CONSUMER = {
        {CAN_CONSUMER + CAN_OWNER + CAN_SET_IN_MSG + CAN_SEND + CAN_STATUS_ACTIVE, 1, -1, -1, -1 },
        {CAN_CONSUMER, 2, 3, 4, 4 },
        {CAN_CONSUMER + CAN_OWNER + CAN_SEND + CAN_SET_FAULT_MSG + CAN_STATUS_ACTIVE
                    + CAN_STATUS_ERROR + CAN_STATUS_DONE, -1, 5, 6, 6 }, 
        {CAN_CONSUMER + CAN_OWNER + CAN_SEND + CAN_STATUS_ERROR + CAN_STATUS_DONE, -1, -1, 6, 6 },
        {CAN_CONSUMER + CAN_OWNER, -1, -1, -1, -1 }, 
        {CAN_CONSUMER, -1, -1, 7, 7 }, 
        {CAN_CONSUMER, -1, -1, -1, -1 },
        {CAN_CONSUMER + CAN_OWNER, -1, -1, -1, -1 }
    };

    private static final int[][] STATES_PROVIDER = {
        {CAN_PROVIDER, 1, -1, -1 },
        {CAN_PROVIDER + CAN_OWNER + CAN_SET_OUT_MSG + CAN_SET_FAULT_MSG + CAN_SEND
                    + CAN_STATUS_ACTIVE + CAN_STATUS_ERROR + CAN_STATUS_DONE, 2, 3, 4, 4 }, 
        {CAN_PROVIDER, -1, 5, 6, 6 },
        {CAN_PROVIDER, -1, -1, 6, 6 }, 
        {CAN_PROVIDER, -1, -1, -1, -1 },
        {CAN_PROVIDER + CAN_OWNER + CAN_SEND + CAN_STATUS_ERROR + CAN_STATUS_DONE, -1, -1, 7, 7 },
        {CAN_PROVIDER + CAN_OWNER, -1, -1, -1, -1 }, 
        {CAN_PROVIDER, -1, -1, -1, -1 }
    };

    public InOptionalOutImpl() {
    }

    public InOptionalOutImpl(String exchangeId) {
        super(exchangeId, MessageExchangeSupport.IN_OPTIONAL_OUT, STATES_CONSUMER);
        this.mirror = new InOptionalOutImpl(this);
    }

    public InOptionalOutImpl(ExchangePacket packet) {
        super(packet, STATES_CONSUMER);
        this.mirror = new InOptionalOutImpl(this);
    }

    protected InOptionalOutImpl(InOptionalOutImpl mep) {
        super(mep.packet, STATES_PROVIDER);
        this.mirror = mep;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.packet = new ExchangePacket();
        this.packet.readExternal(in);
        if (this.packet.in != null) {
            this.packet.in.exchange = this;
        }
        if (this.packet.out != null) {
            this.packet.out.exchange = this;
        }
        if (this.packet.fault != null) {
            this.packet.fault.exchange = this;
        }
        this.state = in.read();
        this.mirror = new InOptionalOutImpl();
        this.mirror.mirror = this;
        this.mirror.packet = this.packet;
        this.mirror.state = in.read();
        boolean provider = in.readBoolean();
        if (provider) {
            this.states = STATES_PROVIDER;
            this.mirror.states = STATES_CONSUMER;
        } else {
            this.states = STATES_CONSUMER;
            this.mirror.states = STATES_PROVIDER;
        }
    }

}