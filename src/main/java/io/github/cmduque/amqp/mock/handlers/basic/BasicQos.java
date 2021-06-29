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
package io.github.cmduque.amqp.mock.handlers.basic;

import com.rabbitmq.client.impl.AMQImpl;
import io.github.cmduque.amqp.mock.ClientHandler;
import io.github.cmduque.amqp.mock.handlers.IMethodHandler;

import java.io.IOException;

import static io.github.cmduque.amqp.mock.util.IOUtils.writeMethod;

public class BasicQos implements IMethodHandler<AMQImpl.Basic.Qos> {
    @Override
    public void handleMethod(ClientHandler clientHandler, int channelNumber, AMQImpl.Basic.Qos method) throws IOException {
        AMQImpl.Basic.QosOk response = new AMQImpl.Basic.QosOk();
        writeMethod(clientHandler.getDataOutputStream(), channelNumber, response);
    }
}