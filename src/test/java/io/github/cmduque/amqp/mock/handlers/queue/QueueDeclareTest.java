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
package io.github.cmduque.amqp.mock.handlers.queue;

import com.rabbitmq.client.impl.AMQImpl;
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
@PrepareForTest({IOUtils.class, QueueDeclare.class})
public class QueueDeclareTest {
    @Test
    public void handleMethodMustWriteQueueDeclareOkMethod() throws Exception {
        int channelNumber = 2;
        String queue = "queue";
        ClientHandler clientHandler = mock(ClientHandler.class);
        DataOutputStream dataOutputStream = mock(DataOutputStream.class);
        when(clientHandler.getDataOutputStream()).thenReturn(dataOutputStream);
        AMQImpl.Queue.Declare method = mock(AMQImpl.Queue.Declare.class);
        when(method.getQueue()).thenReturn(queue);
        ServerConfig serverConfig = ServerConfig.defaultConfig();
        when(clientHandler.getServerConfig()).thenReturn(serverConfig);
        mockStatic(IOUtils.class);
        AMQImpl.Queue.DeclareOk response = mock(AMQImpl.Queue.DeclareOk.class);
        whenNew(AMQImpl.Queue.DeclareOk.class).withArguments(queue, 0, 0).thenReturn(response);
        QueueDeclare methodHandler = new QueueDeclare();

        methodHandler.handleMethod(clientHandler, channelNumber, method);

        verifyStatic(IOUtils.class, times(1));
        IOUtils.writeMethod(dataOutputStream, channelNumber, response);
    }
}