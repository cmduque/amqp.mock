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

import com.rabbitmq.client.impl.Method;
import io.github.cmduque.amqp.mock.dto.Message;
import io.github.cmduque.amqp.mock.dto.QueueConsumer;
import io.github.cmduque.amqp.mock.dto.ServerConfig;
import io.github.cmduque.amqp.mock.handlers.IMethodHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class AMQPServerMock {

    @Getter
    private final ServerConfig serverConfig;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private final Random randomConsumer = new Random();
    private final Map<String, List<QueueConsumer>> queueConsumers = new HashMap<>();
    private final Map<String, List<Message>> enqueuedMessages = new HashMap<>();
    private final Map<String, CountDownLatch> lockForMessagesMap = new HashMap<>();

    public AMQPServerMock(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void start() {
        try {
            AMQPServerMock instance = this;
            serverSocket = new ServerSocket(serverConfig.getPort());
            isRunning = true;
            new Thread(() -> {
                log.info("AMQPServerMock started on port {}", serverConfig.getPort());
                while (isRunning) {
                    try {
                        ClientHandler clientHandler = new ClientHandler(instance, serverSocket.accept());
                        handleClient(clientHandler);
                    } catch (IOException ex) {
                        if (isRunning) {
                            log.error("An error occurs running server", ex);
                        }
                    }
                }
            }).start();
        } catch (IOException ex) {
            log.error("An error occurs stating server", ex);
        }
    }

    protected void handleClient(ClientHandler clientHandler) {
        new Thread(clientHandler).start();
    }

    public void stop() {
        isRunning = false;
        try {
            serverSocket.close();
            log.info("AMQPServerMock stopped");
        } catch (IOException ex) {
            log.error("An error occurs stopping server", ex);
        }
    }

    public void publish(Message message) throws IOException {
        List<QueueConsumer> consumerList = queueConsumers.getOrDefault(message.getRoutingKey(), new ArrayList<>());
        if (consumerList.isEmpty()) {
            enqueueMessage(message);
        } else {
            QueueConsumer queueConsumer = consumerList.get(randomConsumer.nextInt(consumerList.size()));
            queueConsumer.getClientHandler().publish(queueConsumer.getChannelNumber(), message);
        }
    }

    /**
     * Return all enqueued messages
     *
     * @param key Must be exchange.routingKey or .queueName
     * @return List with enqueued messages
     */
    public List<Message> getAllReceivedMessages(String key) {
        return enqueuedMessages.getOrDefault(key, new ArrayList<>());
    }

    protected void registerQueueConsumer(QueueConsumer queueConsumer, String queueName) {
        List<QueueConsumer> consumerList = queueConsumers.getOrDefault(queueName, new ArrayList<>());
        consumerList.add(queueConsumer);
        queueConsumers.putIfAbsent(queueName, consumerList);
        Iterator<Message> enqueuedMessagesIterator = enqueuedMessages.getOrDefault(".".concat(queueName), new ArrayList<>()).iterator();
        while (enqueuedMessagesIterator.hasNext()) {
            Message message = enqueuedMessagesIterator.next();
            try {
                queueConsumer.getClientHandler().publish(queueConsumer.getChannelNumber(), message);
                enqueuedMessagesIterator.remove();
            } catch (IOException ex) {
                log.error("An error occurs publishing messages", ex);
            }
        }
    }

    protected void enqueueMessage(Message message) {
        String key = String.format("%s.%s", message.getExchange(), message.getRoutingKey());
        List<Message> messageList = enqueuedMessages.getOrDefault(key, new ArrayList<>());
        messageList.add(message);
        enqueuedMessages.putIfAbsent(key, messageList);
        countMessage(key);
    }

    public void putCustomHandler(Class<? extends Method> method, IMethodHandler<? extends Method> methodHandler) {
        ClientHandler.METHOD_HANDLERS_MAP.put(method, methodHandler);
    }

    public CountDownLatch getLockForMessages(String key, int count) {
        CountDownLatch lockForMessage = new CountDownLatch(count);
        lockForMessagesMap.put(key, lockForMessage);
        return lockForMessage;
    }

    protected void countMessage(String key) {
        Optional.ofNullable(lockForMessagesMap.get(key))
                .ifPresent(CountDownLatch::countDown);
    }
}