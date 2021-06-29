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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.impl.AMQImpl;
import com.rabbitmq.client.impl.Frame;
import com.rabbitmq.client.impl.Method;
import io.github.cmduque.amqp.mock.dto.Message;
import io.github.cmduque.amqp.mock.dto.QueueConsumer;
import io.github.cmduque.amqp.mock.dto.ServerConfig;
import io.github.cmduque.amqp.mock.handlers.IMethodHandler;
import io.github.cmduque.amqp.mock.handlers.NotImplementHandler;
import io.github.cmduque.amqp.mock.handlers.basic.*;
import io.github.cmduque.amqp.mock.handlers.channel.ChannelClose;
import io.github.cmduque.amqp.mock.handlers.channel.ChannelOpen;
import io.github.cmduque.amqp.mock.handlers.connection.*;
import io.github.cmduque.amqp.mock.handlers.exchange.ExchangeDeclare;
import io.github.cmduque.amqp.mock.handlers.queue.QueueBind;
import io.github.cmduque.amqp.mock.handlers.queue.QueueDeclare;
import io.github.cmduque.amqp.mock.handlers.tx.TxCommit;
import io.github.cmduque.amqp.mock.handlers.tx.TxSelect;
import io.github.cmduque.amqp.mock.util.IOUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ClientHandler implements Runnable {

    private final AMQPServerMock server;
    @Getter
    private final ServerConfig serverConfig;
    @Getter
    private final DataInputStream dataInputStream;
    @Getter
    private final DataOutputStream dataOutputStream;
    protected static final Map<Class<? extends Method>, IMethodHandler> METHOD_HANDLERS_MAP = new HashMap<>();
    private boolean isRunning = false;

    static {
        METHOD_HANDLERS_MAP.put(AMQImpl.Basic.Ack.class, new BasicAck());
        METHOD_HANDLERS_MAP.put(AMQImpl.Basic.Cancel.class, new BasicCancel());
        METHOD_HANDLERS_MAP.put(AMQImpl.Basic.Consume.class, new BasicConsume());
        METHOD_HANDLERS_MAP.put(AMQImpl.Basic.Publish.class, new BasicPublish());
        METHOD_HANDLERS_MAP.put(AMQImpl.Basic.Qos.class, new BasicQos());
        METHOD_HANDLERS_MAP.put(AMQImpl.Channel.Open.class, new ChannelOpen());
        METHOD_HANDLERS_MAP.put(AMQImpl.Channel.Close.class, new ChannelClose());
        METHOD_HANDLERS_MAP.put(AMQImpl.Connection.Close.class, new ConnectionClose());
        METHOD_HANDLERS_MAP.put(AMQImpl.Connection.Open.class, new ConnectionOpen());
        METHOD_HANDLERS_MAP.put(AMQImpl.Connection.StartOk.class, new ConnectionStartOk());
        METHOD_HANDLERS_MAP.put(AMQImpl.Connection.TuneOk.class, new ConnectionTuneOk());
        METHOD_HANDLERS_MAP.put(AMQImpl.Exchange.Declare.class, new ExchangeDeclare());
        METHOD_HANDLERS_MAP.put(AMQImpl.Queue.Declare.class, new QueueDeclare());
        METHOD_HANDLERS_MAP.put(AMQImpl.Queue.Bind.class, new QueueBind());
        METHOD_HANDLERS_MAP.put(AMQImpl.Tx.Commit.class, new TxCommit());
        METHOD_HANDLERS_MAP.put(AMQImpl.Tx.Select.class, new TxSelect());
    }

    protected ClientHandler(AMQPServerMock server, Socket socket) throws IOException {
        this.server = server;
        this.serverConfig = server.getServerConfig();
        dataInputStream = new DataInputStream(socket.getInputStream());
        dataOutputStream = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        try {
            startConnection();
            isRunning = true;
            while (isRunning) {
                Frame frame = Frame.readFrom(dataInputStream);
                switch (frame.type) {
                    case AMQP.FRAME_METHOD:
                        Method method = IOUtils.readMethod(frame.getInputStream());
                        METHOD_HANDLERS_MAP.getOrDefault(method.getClass(), new NotImplementHandler())
                                .handleMethod(this, frame.channel, method);
                        break;
                    case AMQP.FRAME_HEARTBEAT:
                        log.info("Heartbeat received");
                        break;
                    default:
                        throw new IOException(String.format("Frame %s not supported", frame.toString()));
                }
            }
        } catch (EOFException ex) {
            if (isRunning) {
                log.error("Channel is closed", ex);
            }
        } catch (Exception ex) {
            if (isRunning) {
                log.error("Unhandled exception occurs", ex);
            }
        }
    }

    public void stop() {
        try {
            isRunning = false;
            dataInputStream.close();
        } catch (IOException e) {}
    }

    protected void startConnection() throws IOException {
        log.debug("received --> Connection Open");
        dataInputStream.readFully(new byte[8]); // Read protocol declaration
        new ConnectionStart().handleMethod(this, 0, null);
    }

    protected void publish(int channelNumber, Message message) throws IOException {
        AMQImpl.Basic.Deliver basicDeliver = new AMQImpl.Basic.Deliver(serverConfig.getConsumerTag(), serverConfig.getDeliveryTag(), false, message.getExchange(), message.getRoutingKey());
        Frame body = new Frame(AMQP.FRAME_BODY, channelNumber, message.getBody());
        IOUtils.writeFrames(dataOutputStream,
                basicDeliver.toFrame(channelNumber),
                message.getContentHeader().toFrame(channelNumber, message.getBody().length),
                body);
    }

    public void registerQueueConsumer(int channelNumber, String queueName) {
        server.registerQueueConsumer(new QueueConsumer(channelNumber, this), queueName);
    }

    public void enqueueMessage(Message message) throws IOException {
        server.publish(message);
    }
}