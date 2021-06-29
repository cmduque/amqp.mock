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
package io.github.cmduque.amqp.mock;

import com.rabbitmq.client.impl.AMQImpl;
import com.rabbitmq.client.impl.Method;
import io.github.cmduque.amqp.mock.dto.Message;
import io.github.cmduque.amqp.mock.dto.QueueConsumer;
import io.github.cmduque.amqp.mock.dto.ServerConfig;
import io.github.cmduque.amqp.mock.handlers.IMethodHandler;
import io.github.cmduque.amqp.mock.util.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static org.powermock.reflect.internal.WhiteboxImpl.getInternalState;
import static org.powermock.reflect.internal.WhiteboxImpl.setInternalState;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IOUtils.class, AMQPServerMock.class})
public class AMQPServerMockTest {

    private AMQPServerMock server;
    private ServerConfig serverConfig;

    @Before
    public void beforeMethod() {
        serverConfig = ServerConfig.defaultConfig();
        server = spy(new AMQPServerMock(serverConfig));
    }

    @Test
    public void startMustCreateServerSocketSetIsRunningAndHandleClient() throws Exception {
        ServerSocket serverSocket = mock(ServerSocket.class);
        whenNew(ServerSocket.class).withArguments(serverConfig.getPort()).thenReturn(serverSocket);
        Socket socket = mock(Socket.class);
        when(serverSocket.accept()).thenAnswer(answer -> {
            server.stop();
            return socket;
        });
        ClientHandler clientHandler = mock(ClientHandler.class);
        whenNew(ClientHandler.class).withArguments(server, socket).thenReturn(clientHandler);
        doNothing().when(server).handleClient(clientHandler);

        server.start();

        verify(serverSocket, timeout(500).times(1)).accept();
        verify(server, timeout(500).times(1)).handleClient(clientHandler);
    }

    @Test
    public void runServerMustCreateNewServerThread1AndHandleClient() throws Exception {
        ClientHandler clientHandler = mock(ClientHandler.class);
        Thread clientThread = mock(Thread.class);
        whenNew(Thread.class).withArguments(clientHandler).thenReturn(clientThread);

        server.handleClient(clientHandler);

        verify(clientThread, times(1)).start();
    }

    @Test
    public void stopMustSetIsRunningToFalseAndCloseServerSocket() throws IOException {
        setInternalState(server, "isRunning", true);
        ServerSocket serverSocket = mock(ServerSocket.class);
        setInternalState(server, "serverSocket", serverSocket);

        server.stop();

        assertFalse(getInternalState(server, "isRunning"));
        verify(serverSocket, times(1)).close();
    }

    @Test
    public void publishMustEnqueueMessageWhenConsumerListIsEmpty() throws IOException {
        String routingKey = "routingKey";
        Message message = new Message(null, routingKey, null, null);
        doNothing().when(server).enqueueMessage(message);

        server.publish(message);

        verify(server, times(1)).enqueueMessage(message);
    }

    @Test
    public void publishMustPublishMessageWhenConsumerListHasElements() throws IOException {
        int channelNumber = 123;
        ClientHandler clientHandler = mock(ClientHandler.class);
        QueueConsumer queueConsumer = spy(new QueueConsumer(channelNumber, clientHandler));
        String routingKey = "routingKey";
        server.registerQueueConsumer(queueConsumer, routingKey);
        Message message = new Message(null, routingKey, null, null);

        server.publish(message);

        verify(queueConsumer, times(1)).getClientHandler();
        verify(clientHandler, times(1)).publish(channelNumber, message);
    }

    @Test
    public void getAllReceivedMessagesMustReturnAListWithEnqueuedMessages() {
        String key = "key";
        List<Message> expectedResult = mock(List.class);
        Map<String, List<Message>> enqueuedMessages = new HashMap<>();
        enqueuedMessages.put(key, expectedResult);
        setInternalState(server, "enqueuedMessages", enqueuedMessages);

        List<Message> result = server.getAllReceivedMessages(key);

        assertEquals(expectedResult, result);
    }

    @Test
    public void getAllReceivedMessagesMustReturnAnEmptyListWithNoEnqueuedMessages() {
        String key = "key";
        Map<String, List<Message>> enqueuedMessages = new HashMap<>();
        setInternalState(server, "enqueuedMessages", enqueuedMessages);

        List<Message> result = server.getAllReceivedMessages(key);

        assertTrue(result.isEmpty());
    }

    @Test
    public void registerQueueConsumerMustCreateListAndAddQueueConsumerWhenNotExistMoreConsumers() {
        QueueConsumer queueConsumer = mock(QueueConsumer.class);
        String queueName = "queueName";

        server.registerQueueConsumer(queueConsumer, queueName);

        Map<String, List<QueueConsumer>> queueConsumers = getInternalState(server, "queueConsumers");
        assertEquals(1, queueConsumers.get(queueName).size());
    }

    @Test
    public void registerQueueConsumerMustGetListAndAddQueueConsumerWhenExistMoreConsumers() {
        QueueConsumer queueConsumer1 = mock(QueueConsumer.class);
        QueueConsumer queueConsumer2 = mock(QueueConsumer.class);
        String queueName = "queueName";

        server.registerQueueConsumer(queueConsumer1, queueName);
        server.registerQueueConsumer(queueConsumer2, queueName);

        Map<String, List<QueueConsumer>> queueConsumers = getInternalState(server, "queueConsumers");
        assertEquals(2, queueConsumers.get(queueName).size());
    }

    @Test
    public void registerQueueConsumerMustCreateListAndAddQueueConsumerAndPublishEnqueuedMessages() throws IOException {
        int channelNumber = 345;
        ClientHandler clientHandler = mock(ClientHandler.class);
        QueueConsumer queueConsumer = spy(new QueueConsumer(channelNumber, clientHandler));
        String exchange = "";
        String queueName = "queueName";
        Message message1 = mock(Message.class);
        when(message1.getExchange()).thenReturn(exchange);
        when(message1.getRoutingKey()).thenReturn(queueName);
        Message message2 = mock(Message.class);
        when(message2.getExchange()).thenReturn(exchange);
        when(message2.getRoutingKey()).thenReturn(queueName);
        server.enqueueMessage(message1);
        server.enqueueMessage(message2);

        server.registerQueueConsumer(queueConsumer, queueName);

        Map<String, List<QueueConsumer>> queueConsumers = getInternalState(server, "queueConsumers");
        assertEquals(1, queueConsumers.get(queueName).size());
        verify(clientHandler, times(1)).publish(channelNumber, message1);
        verify(clientHandler, times(1)).publish(channelNumber, message2);
        verifyNoMoreInteractions(clientHandler);
    }

    @Test
    public void enqueueMessageMustCreateListAndAddWhenNotExistEnqueuedMessagesAndCountMessage() {
        String exchange = "exchange";
        String routingKey = "routingKey";
        String key = exchange + "." + routingKey;
        Message message = mock(Message.class);
        when(message.getExchange()).thenReturn(exchange);
        when(message.getRoutingKey()).thenReturn(routingKey);
        doNothing().when(server).countMessage(key);

        server.enqueueMessage(message);

        Map<String, List<Message>> enqueuedMessages = getInternalState(server, "enqueuedMessages");
        assertEquals(1, enqueuedMessages.get(key).size());
        verify(server, times(1)).countMessage(key);
    }

    @Test
    public void enqueueMessageMustGetListAndAddWhenExistEnqueuedMessagesAndCountMessage() {
        String exchange = "exchange";
        String routingKey = "routingKey";
        String key = exchange + "." + routingKey;
        Message message1 = mock(Message.class);
        when(message1.getExchange()).thenReturn(exchange);
        when(message1.getRoutingKey()).thenReturn(routingKey);
        Message message2 = mock(Message.class);
        when(message2.getExchange()).thenReturn(exchange);
        when(message2.getRoutingKey()).thenReturn(routingKey);
        doNothing().when(server).countMessage(key);

        server.enqueueMessage(message1);
        server.enqueueMessage(message2);

        Map<String, List<Message>> enqueuedMessages = getInternalState(server, "enqueuedMessages");
        assertEquals(2, enqueuedMessages.get(key).size());
        verify(server, times(2)).countMessage(key);
    }

    @Test
    public void putCustomHandlerMustPutHandlerOnClientHandler() {
        Class<? extends Method> method = AMQImpl.Basic.Ack.class;
        IMethodHandler methodHandler = mock(IMethodHandler.class);

        server.putCustomHandler(method, methodHandler);

        assertEquals(methodHandler, ClientHandler.METHOD_HANDLERS_MAP.get(method));
    }

    @Test
    public void getLockForMessagesCreateNewCountDownAndPutAndReturn() throws Exception {
        String key = "key";
        int count = 1;
        CountDownLatch expectedResult = mock(CountDownLatch.class);
        whenNew(CountDownLatch.class).withArguments(count).thenReturn(expectedResult);

        CountDownLatch result = server.getLockForMessages(key, count);

        assertEquals(expectedResult, result);
        Map<String, CountDownLatch> lockForMessagesMap = getInternalState(server, "lockForMessagesMap");
        assertEquals(expectedResult, lockForMessagesMap.get(key));
    }

    @Test
    public void countMessageMustCountMessageWhenExistLock() {
        String key = "key";
        CountDownLatch countDownLatch = mock(CountDownLatch.class);
        Map<String, CountDownLatch> lockForMessagesMap = getInternalState(server, "lockForMessagesMap");
        lockForMessagesMap.put(key, countDownLatch);

        server.countMessage(key);

        verify(countDownLatch, times(1)).countDown();
    }
}