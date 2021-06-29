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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.impl.AMQImpl;
import com.rabbitmq.client.impl.Frame;
import io.github.cmduque.amqp.mock.ClientHandler;
import io.github.cmduque.amqp.mock.dto.Message;
import io.github.cmduque.amqp.mock.handlers.IMethodHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.IOException;

import static io.github.cmduque.amqp.mock.util.IOUtils.readContentHeader;

@Slf4j
public class BasicPublish implements IMethodHandler<AMQImpl.Basic.Publish> {
    @Override
    public void handleMethod(ClientHandler clientHandler, int channelNumber, AMQImpl.Basic.Publish method) throws IOException {
        DataInputStream dataInputStream = clientHandler.getDataInputStream();
        AMQP.BasicProperties contentHeader = readContentHeader(dataInputStream);
        Frame contentBodyFrame = Frame.readFrom(dataInputStream);
        byte[] body = contentBodyFrame.getPayload();
        log.debug("received --> {}", body);

        clientHandler.enqueueMessage(new Message(method.getExchange(), method.getRoutingKey(), contentHeader, body));
    }
}