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

import com.rabbitmq.client.impl.Method;
import io.github.cmduque.amqp.mock.handlers.channel.Heartbeat;
import lombok.extern.slf4j.Slf4j;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static io.github.cmduque.amqp.mock.util.IOUtils.writeMethod;

@Slf4j
public class HeartbeatTimerTask extends TimerTask {
    private final Timer timer;
    private final DataOutputStream dataOutputStream;

    public HeartbeatTimerTask(Timer timer, DataOutputStream dataOutputStream) {
        this.timer = timer;
        this.dataOutputStream = dataOutputStream;
    }

    public void run() {
        Method method = new Heartbeat();
        try {
            writeMethod(dataOutputStream, 0, method);
        } catch (IOException e) {
            timer.cancel();
            log.warn("An error occurs sending heartbeat. Timer was canceled");
        }
    }
}