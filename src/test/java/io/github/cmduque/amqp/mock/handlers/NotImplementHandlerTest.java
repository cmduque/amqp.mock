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
package io.github.cmduque.amqp.mock.handlers;

import com.rabbitmq.client.impl.Method;
import io.github.cmduque.amqp.mock.ClientHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
public class NotImplementHandlerTest {
    @Test
    public void handleMethodMustThrowAnIOExceptionWithMessage() {
        ClientHandler clientHandler = null;
        int channelNumber = 2;
        Method method = mock(Method.class);
        NotImplementHandler methodHandler = new NotImplementHandler();
        Exception thrownException = null;

        try {
            methodHandler.handleMethod(clientHandler, channelNumber, method);
            fail();
        } catch (Exception ex) {
            thrownException = ex;
        }

        assertTrue(thrownException.getMessage().startsWith("Method Mock for Method, "));
        assertTrue(thrownException.getMessage().endsWith(" not supported"));
    }
}