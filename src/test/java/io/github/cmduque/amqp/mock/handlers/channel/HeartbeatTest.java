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
import com.rabbitmq.client.impl.Frame;
import com.rabbitmq.client.impl.LongStringHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LongStringHelper.class, Heartbeat.class})
public class HeartbeatTest {
    private Heartbeat heartbeat;

    @Before
    public void afterMethod() {
        heartbeat = new Heartbeat();
    }

    @Test
    public void protocolClassIdMustBeZero() {

        int result = heartbeat.protocolClassId();

        assertEquals(0, result);
    }

    @Test
    public void protocolMethodIdMustBeZero() {

        int result = heartbeat.protocolMethodId();

        assertEquals(0, result);
    }

    @Test
    public void protocolMethodNameMustBeHeartbeat() {

        String result = heartbeat.protocolMethodName();

        assertEquals("heartbeat", result);
    }

    @Test
    public void hasContentMustBeFalse() {

        boolean result = heartbeat.hasContent();

        assertFalse(result);
    }

    @Test
    public void visitMustReturnNull() {

        Object result = heartbeat.visit(null);

        assertNull(result);
    }

    @Test
    public void toFrameMustReturnANewFrameNull() throws Exception {
        int channelNumber = 1;
        Frame expectedResult = mock(Frame.class);
        whenNew(Frame.class).withArguments(AMQP.FRAME_HEARTBEAT, channelNumber).thenReturn(expectedResult);

        Frame result = heartbeat.toFrame(channelNumber);

        assertEquals(expectedResult, result);
    }
}