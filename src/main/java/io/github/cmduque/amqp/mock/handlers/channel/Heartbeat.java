/**
 * Copyright 2020 - AMQP Mock contributors (https://github.com/cmduque/amqp.mock/graphs/contributors)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.cmduque.amqp.mock.handlers.channel;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.impl.AMQImpl.MethodVisitor;
import com.rabbitmq.client.impl.Frame;
import com.rabbitmq.client.impl.Method;
import com.rabbitmq.client.impl.MethodArgumentWriter;

public class Heartbeat extends Method {

    public int protocolClassId() {
        return 0;
    }

    public int protocolMethodId() {
        return 0;
    }

    public String protocolMethodName() {
        return "heartbeat";
    }

    public boolean hasContent() {
        return false;
    }

    public Object visit(MethodVisitor visitor) {
        return null;
    }

    public void writeArgumentsTo(MethodArgumentWriter writer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Frame toFrame(int channelNumber) {
        return new Frame(AMQP.FRAME_HEARTBEAT, channelNumber);
    }
}