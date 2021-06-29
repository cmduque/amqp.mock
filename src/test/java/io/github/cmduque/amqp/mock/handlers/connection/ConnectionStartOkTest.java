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

import com.rabbitmq.client.impl.AMQImpl;
import io.github.cmduque.amqp.mock.ClientHandler;
import io.github.cmduque.amqp.mock.dto.ServerConfig;
import io.github.cmduque.amqp.mock.util.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.DataOutputStream;
import java.util.Timer;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IOUtils.class, Timer.class, HeartbeatTimerTask.class, ConnectionStartOk.class})
public class ConnectionStartOkTest {
    @Test
    public void handleMethodMustWriteConnectionTuneMethodAndScheduleHeartbeatTimerTask() throws Exception {
        int channelNumber = 2;
        ClientHandler clientHandler = mock(ClientHandler.class);
        DataOutputStream dataOutputStream = mock(DataOutputStream.class);
        when(clientHandler.getDataOutputStream()).thenReturn(dataOutputStream);
        ServerConfig serverConfig = ServerConfig.defaultConfig();
        when(clientHandler.getServerConfig()).thenReturn(serverConfig);
        mockStatic(IOUtils.class);
        AMQImpl.Connection.Tune response = mock(AMQImpl.Connection.Tune.class);
        whenNew(AMQImpl.Connection.Tune.class).withArguments(0, 131072, serverConfig.getHeartbeatIntervalInSeconds()).thenReturn(response);
        ConnectionStartOk methodHandler = new ConnectionStartOk();
        Timer timer = mock(Timer.class);
        whenNew(Timer.class).withArguments(true).thenReturn(timer);
        HeartbeatTimerTask heartbeatTimerTask = mock(HeartbeatTimerTask.class);
        whenNew(HeartbeatTimerTask.class).withArguments(timer, dataOutputStream).thenReturn(heartbeatTimerTask);

        methodHandler.handleMethod(clientHandler, channelNumber, null);

        verifyStatic(IOUtils.class, times(1));
        IOUtils.writeMethod(dataOutputStream, 0, response);
        verify(timer, times(1)).scheduleAtFixedRate(heartbeatTimerTask, 0, serverConfig.getHeartbeatIntervalInSeconds() * 1000L);
    }
}