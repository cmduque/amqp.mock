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
import io.github.cmduque.amqp.mock.dto.ServerConfig;
import io.github.cmduque.amqp.mock.util.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IOUtils.class, Frame.class, BasicPublish.class})
public class BasicPublishTest {
    @Test
    public void handleMethodMustReadContentHeaderAndContentBodyAndEnqueueMessage() throws Exception {
        int channelNumber = 2;
        String exchange = "exchange";
        String routingKey = "routingKey";
        ClientHandler clientHandler = mock(ClientHandler.class);
        DataInputStream dataInputStream = mock(DataInputStream.class);
        when(clientHandler.getDataInputStream()).thenReturn(dataInputStream);
        DataOutputStream dataOutputStream = mock(DataOutputStream.class);
        when(clientHandler.getDataOutputStream()).thenReturn(dataOutputStream);
        AMQImpl.Basic.Publish method = new AMQImpl.Basic.Publish(0, exchange, routingKey, false, false);
        ServerConfig serverConfig = ServerConfig.defaultConfig();
        when(clientHandler.getServerConfig()).thenReturn(serverConfig);
        mockStatic(IOUtils.class);
        long bodySize = 12438;
        AMQP.BasicProperties contentHeader = mock(AMQP.BasicProperties.class);
        when(contentHeader.getBodySize()).thenReturn(bodySize);
        when(IOUtils.readContentHeader(dataInputStream)).thenReturn(contentHeader);
        byte[] body = new byte[0];
        Frame contentBodyFrame = mock(Frame.class);
        when(contentBodyFrame.getPayload()).thenReturn(body);
        mockStatic(Frame.class);
        when(Frame.readFrom(dataInputStream)).thenReturn(contentBodyFrame);
        BasicPublish methodHandler = new BasicPublish();
        Message message = mock(Message.class);
        whenNew(Message.class).withArguments(exchange, routingKey, contentHeader, body).thenReturn(message);

        methodHandler.handleMethod(clientHandler, channelNumber, method);

        verifyStatic(IOUtils.class, times(1));
        IOUtils.readContentHeader(dataInputStream);
        verifyStatic(Frame.class, times(1));
        Frame.readFrom(dataInputStream);
        verify(clientHandler, times(1)).enqueueMessage(message);
    }
}