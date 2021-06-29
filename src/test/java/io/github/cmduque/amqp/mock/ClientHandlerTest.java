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
import io.github.cmduque.amqp.mock.handlers.connection.ConnectionClose;
import io.github.cmduque.amqp.mock.handlers.connection.ConnectionStart;
import io.github.cmduque.amqp.mock.util.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.*;
import java.net.Socket;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.powermock.reflect.internal.WhiteboxImpl.getInternalState;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IOUtils.class, Frame.class, ClientHandler.class})
public class ClientHandlerTest {

    private AMQPServerMock server;
    private ClientHandler clientHandler;
    private InputStream inputStream;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    @Before
    public void beforeMethod() throws Exception {
        inputStream = mock(InputStream.class);
        OutputStream outputStream = mock(OutputStream.class);
        dataInputStream = mock(DataInputStream.class);
        whenNew(DataInputStream.class).withArguments(inputStream).thenReturn(dataInputStream);
        dataOutputStream = mock(DataOutputStream.class);
        whenNew(DataOutputStream.class).withArguments(outputStream).thenReturn(dataOutputStream);
        ServerConfig serverConfig = ServerConfig.defaultConfig();
        server = mock(AMQPServerMock.class);
        when(server.getServerConfig()).thenReturn(serverConfig);
        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenReturn(inputStream);
        when(socket.getOutputStream()).thenReturn(outputStream);
        clientHandler = PowerMockito.spy(new ClientHandler(server, socket));
    }

    @Test
    public void runMustStartConnectionAndReadFrameAndReadMethodWhenFrameTypeIsMethodAndWhileIsRunning() throws IOException {
        doNothing().when(clientHandler).startConnection();
        Frame frame = spy(new Frame(AMQP.FRAME_METHOD, 0));
        doReturn(dataInputStream).when(frame).getInputStream();
        mockStatic(Frame.class);
        when(Frame.readFrom(dataInputStream)).thenReturn(frame);
        mockStatic(IOUtils.class);
        AMQImpl.Connection.Close connectionClose = new AMQImpl.Connection.Close(0, "", 0, 0);
        when(IOUtils.readMethod(dataInputStream)).thenReturn(connectionClose);
        IMethodHandler methodHandler = (ClientHandler clientHandler, int channelNumber, Method method) -> clientHandler.stop();
        ClientHandler.METHOD_HANDLERS_MAP.put(AMQImpl.Connection.Close.class, methodHandler);

        clientHandler.run();

        verify(clientHandler, times(1)).startConnection();
        verifyStatic(Frame.class, times(1));
        Frame.readFrom(dataInputStream);
        verifyStatic(IOUtils.class, times(1));
        IOUtils.readMethod(dataInputStream);
        ClientHandler.METHOD_HANDLERS_MAP.put(AMQImpl.Connection.Close.class, new ConnectionClose());
    }

    @Test
    public void runMustStartConnectionAndReadFrameAndDoNothingWhenFrameTypeIsHeartbeatAndWhileIsRunning() throws IOException {
        doNothing().when(clientHandler).startConnection();
        Frame frame = spy(new Frame(AMQP.FRAME_HEARTBEAT, 0));
        doReturn(dataInputStream).when(frame).getInputStream();
        mockStatic(Frame.class);
        when(Frame.readFrom(dataInputStream)).then((invocation) -> {
            clientHandler.stop();
            return frame;
        });

        clientHandler.run();

        verify(clientHandler, times(1)).run();
        verify(clientHandler, times(1)).stop();
        verify(clientHandler, times(1)).startConnection();
        verifyStatic(Frame.class, times(1));
        Frame.readFrom(dataInputStream);
        verify(dataInputStream, times(1)).close();
        verifyNoMoreInteractions(clientHandler, frame, dataInputStream, dataOutputStream);
    }

    @Test
    public void runMustCallNotImplementHandlerWhenMethodIsUnknown() throws Exception {
        doNothing().when(clientHandler).startConnection();
        int channelNumber = 2;
        Frame frame = spy(new Frame(AMQP.FRAME_METHOD, channelNumber));
        doReturn(dataInputStream).when(frame).getInputStream();
        mockStatic(Frame.class);
        when(Frame.readFrom(dataInputStream)).thenReturn(frame);
        mockStatic(IOUtils.class);
        Method method = mock(Method.class);
        NotImplementHandler notImplementHandler = spy(new NotImplementHandler() {
            public void handleMethod(ClientHandler clientHandler, int channelNumber, Method method) {
                clientHandler.stop();
            }
        });
        whenNew(NotImplementHandler.class).withNoArguments().thenReturn(notImplementHandler);
        when(IOUtils.readMethod(dataInputStream)).thenReturn(method);

        clientHandler.run();

        verify(clientHandler, times(1)).startConnection();
        verify(notImplementHandler, times(1)).handleMethod(clientHandler, channelNumber, method);
    }

    @Test
    public void runMustThrowAnExceptionWhenFrameIsUnknown() throws Exception {
        doNothing().when(clientHandler).startConnection();
        int channelNumber = 2;
        Frame frame = spy(new Frame(AMQP.FRAME_END, channelNumber));
        doReturn(dataInputStream).when(frame).getInputStream();
        mockStatic(Frame.class);
        when(Frame.readFrom(dataInputStream)).thenReturn(frame);
        IOException exception = mock(IOException.class);
        whenNew(IOException.class).withAnyArguments().thenReturn(exception);

        clientHandler.run();

        verify(clientHandler, times(1)).startConnection();
        verify(exception, times(1)).printStackTrace(any(PrintStream.class));
    }

    @Test
    public void stopMustSetIsRunningToFalseAndCloseDataInputStream() throws IOException {
        clientHandler.stop();

        boolean isRunning = getInternalState(clientHandler, "isRunning");
        assertFalse(isRunning);
        verify(dataInputStream, times(1)).close();
    }

    @Test
    public void startConnectionMustReadOffsetAndHandleConnectionStart() throws Exception {
        int channelNumber = 2;
        int offset = 8;
        ConnectionStart connectionStart = mock(ConnectionStart.class);
        whenNew(ConnectionStart.class).withNoArguments().thenReturn(connectionStart);

        clientHandler.startConnection();

        when(inputStream.read(new byte[8], 0, offset)).thenReturn(offset);
        verify(connectionStart, times(1)).handleMethod(clientHandler, 0, null);
    }

    @Test
    public void publishMustWriteFramesDeliverContentHeaderAndBodyToDataOutputStream() throws Exception {
        int channelNumber = 2;
        String queueName = "queueName";
        byte[] payload = new byte[1];
        AMQImpl.Basic.Deliver basicDeliver = mock(AMQImpl.Basic.Deliver.class);
        whenNew(AMQImpl.Basic.Deliver.class).withAnyArguments().thenReturn(basicDeliver);
        Frame basicDeliverFrame = mock(Frame.class);
        when(basicDeliver.toFrame(channelNumber)).thenReturn(basicDeliverFrame);
        AMQP.BasicProperties contentHeader = mock(AMQP.BasicProperties.class);
        Frame contentHeaderFrame = mock(Frame.class);
        when(contentHeader.toFrame(channelNumber, payload.length)).thenReturn(contentHeaderFrame);
        Frame body = mock(Frame.class);
        whenNew(Frame.class).withArguments(AMQP.FRAME_BODY, channelNumber, payload).thenReturn(body);
        mockStatic(IOUtils.class);
        Message message = new Message("", queueName, contentHeader, payload);

        clientHandler.publish(channelNumber, message);

        verifyStatic(IOUtils.class);
        IOUtils.writeFrames(dataOutputStream,
                basicDeliver.toFrame(channelNumber),
                contentHeader.toFrame(channelNumber, payload.length),
                body);
    }

    @Test
    public void registerQueueConsumerMustRegisterQueueConsumerInTheServer() throws Exception {
        int channelNumber = 2;
        String queueName = "queueName";
        QueueConsumer queueConsumer = new QueueConsumer(channelNumber, clientHandler);
        whenNew(QueueConsumer.class).withArguments(channelNumber, clientHandler).thenReturn(queueConsumer);

        clientHandler.registerQueueConsumer(channelNumber, queueName);

        verify(server, times(1)).registerQueueConsumer(queueConsumer, queueName);
    }

    @Test
    public void enqueueMessageMustPublishMessageInTheServer() throws IOException {
        Message message = mock(Message.class);

        clientHandler.enqueueMessage(message);

        verify(server, times(1)).publish(message);
    }
}