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
import io.github.cmduque.amqp.mock.handlers.IMethodHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static io.github.cmduque.amqp.mock.util.IOUtils.writeMethod;

@Slf4j
public class ConnectionStartOk implements IMethodHandler<AMQImpl.Connection.StartOk> {
    @Override
    public void handleMethod(ClientHandler clientHandler, int channelNumber, AMQImpl.Connection.StartOk method) throws IOException {
        ServerConfig serverConfig = clientHandler.getServerConfig();
        AMQImpl.Connection.Tune response = new AMQImpl.Connection.Tune(0, 131072, serverConfig.getHeartbeatIntervalInSeconds());
        writeMethod(clientHandler.getDataOutputStream(), 0, response);

        Timer timer = new Timer(true);
        TimerTask heartbeatTimerTask = new HeartbeatTimerTask(timer, clientHandler.getDataOutputStream());
        timer.scheduleAtFixedRate(heartbeatTimerTask, 0, serverConfig.getHeartbeatIntervalInSeconds() * 1000L);
    }
}