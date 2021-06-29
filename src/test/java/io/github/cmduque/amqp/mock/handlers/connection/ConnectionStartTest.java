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
package io.github.cmduque.amqp.mock.handlers.connection;

import com.rabbitmq.client.LongString;
import com.rabbitmq.client.impl.AMQConnection;
import com.rabbitmq.client.impl.AMQImpl;
import com.rabbitmq.client.impl.LongStringHelper;
import io.github.cmduque.amqp.mock.ClientHandler;
import io.github.cmduque.amqp.mock.dto.ServerConfig;
import io.github.cmduque.amqp.mock.util.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.DataOutputStream;

import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IOUtils.class, LongStringHelper.class, ConnectionStart.class})
public class ConnectionStartTest {
    @Test
    public void handleMethodMustWriteConnectionStartMethod() throws Exception {
        int channelNumber = 2;
        ClientHandler clientHandler = mock(ClientHandler.class);
        DataOutputStream dataOutputStream = mock(DataOutputStream.class);
        when(clientHandler.getDataOutputStream()).thenReturn(dataOutputStream);
        ServerConfig serverConfig = ServerConfig.defaultConfig();
        when(clientHandler.getServerConfig()).thenReturn(serverConfig);
        LongString mechanisms = mock(LongString.class);
        LongString encodes = mock(LongString.class);
        mockStatic(LongStringHelper.class);
        when(LongStringHelper.asLongString("AMQPLAIN PLAIN")).thenReturn(mechanisms);
        when(LongStringHelper.asLongString("en_US")).thenReturn(encodes);
        mockStatic(IOUtils.class);
        AMQImpl.Connection.Start response = mock(AMQImpl.Connection.Start.class);
        whenNew(AMQImpl.Connection.Start.class).withArguments(0, 9, AMQConnection.defaultClientProperties(), mechanisms, encodes).thenReturn(response);
        ConnectionStart methodHandler = new ConnectionStart();

        methodHandler.handleMethod(clientHandler, channelNumber, null);

        verifyStatic(IOUtils.class, times(1));
        IOUtils.writeMethod(dataOutputStream, 0, response);
    }
}