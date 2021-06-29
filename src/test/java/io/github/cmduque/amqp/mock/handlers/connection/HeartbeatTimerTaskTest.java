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

import io.github.cmduque.amqp.mock.handlers.channel.Heartbeat;
import io.github.cmduque.amqp.mock.util.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Timer;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IOUtils.class, HeartbeatTimerTask.class})
public class HeartbeatTimerTaskTest {
    @Test
    public void runMustWriteAHeartbeat() throws Exception {
        Timer timer = null;
        DataOutputStream dataOutputStream = mock(DataOutputStream.class);
        Heartbeat method = mock(Heartbeat.class);
        whenNew(Heartbeat.class).withNoArguments().thenReturn(method);
        mockStatic(IOUtils.class);
        HeartbeatTimerTask heartbeatTimerTask = new HeartbeatTimerTask(timer, dataOutputStream);

        heartbeatTimerTask.run();

        verifyStatic(IOUtils.class, times(1));
        IOUtils.writeMethod(dataOutputStream, 0, method);
    }

    @Test
    public void runMustCancelTimerWhenIOExceptionOccurs() throws Exception {
        Timer timer = mock(Timer.class);
        DataOutputStream dataOutputStream = mock(DataOutputStream.class);
        Heartbeat method = mock(Heartbeat.class);
        whenNew(Heartbeat.class).withNoArguments().thenReturn(method);
        mockStatic(IOUtils.class);
        doThrow(new IOException()).when(IOUtils.class, "writeMethod", dataOutputStream, 0, method);
        HeartbeatTimerTask heartbeatTimerTask = new HeartbeatTimerTask(timer, dataOutputStream);

        heartbeatTimerTask.run();

        verify(timer, times(1)).cancel();
    }
}