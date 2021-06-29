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
package io.github.cmduque.amqp.mock.util;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.impl.AMQImpl;
import com.rabbitmq.client.impl.Frame;
import com.rabbitmq.client.impl.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IOUtils.class, Frame.class, AMQImpl.class})
public class IOUtilsTest {

    @Test
    public void readMethodRabbitMustReadOffsetAndReturnMethodFromDataInputStream() throws IOException {
        DataInputStream dataInputStream = mock(DataInputStream.class);
        Method expectedResult = mock(Method.class);
        mockStatic(AMQImpl.class);
        when(AMQImpl.readMethodFrom(dataInputStream)).thenReturn(expectedResult);

        Method result = IOUtils.readMethod(dataInputStream);

        assertEquals(expectedResult, result);
    }

    @Test
    public void readContentHeaderMustReadOffsetAndReturnContentHeaderFromDataInputStream() throws IOException {

        DataInputStream dataInputStream = mock(DataInputStream.class);
        Frame contentHeaderFrame = mock(Frame.class);
        when(contentHeaderFrame.getInputStream()).thenReturn(dataInputStream);
        mockStatic(Frame.class);
        when(Frame.readFrom(dataInputStream)).thenReturn(contentHeaderFrame);
        AMQP.BasicProperties expectedResult = mock(AMQP.BasicProperties.class);
        mockStatic(AMQImpl.class);
        when(AMQImpl.readContentHeaderFrom(dataInputStream)).thenReturn(expectedResult);

        AMQP.BasicProperties result = IOUtils.readContentHeader(dataInputStream);

        verifyStatic(Frame.class);
        Frame.readFrom(dataInputStream);
        assertEquals(expectedResult, result);
    }

    @Test
    public void writeMethodMustGetFrameWithChannelNumberAndWriteFrame() throws IOException {
        OutputStream outputStream = mock(OutputStream.class);
        int channelNumber = 0;
        Method method = mock(Method.class);
        Frame frame = mock(Frame.class);
        when(method.toFrame(channelNumber)).thenReturn(frame);
        mockStatic(IOUtils.class, CALLS_REAL_METHODS);
        doNothing().when(IOUtils.class);
        IOUtils.writeFrames(outputStream, frame);

        IOUtils.writeMethod(outputStream, channelNumber, method);

        verifyStatic(IOUtils.class, times(1));
        IOUtils.writeFrames(outputStream, frame);
    }

    @Test
    public void writeFramesMustWriteAllFramesInDataOutputStreamAndWriteAllBytesInOutputStream() throws Exception {
        OutputStream outputStream = mock(OutputStream.class);
        Frame frame1 = mock(Frame.class);
        Frame frame2 = mock(Frame.class);
        ByteArrayOutputStream byteArrayOutputStream = mock(ByteArrayOutputStream.class);
        whenNew(ByteArrayOutputStream.class).withNoArguments().thenReturn(byteArrayOutputStream);
        DataOutputStream dataOutputStream = mock(DataOutputStream.class);
        whenNew(DataOutputStream.class).withArguments(byteArrayOutputStream).thenReturn(dataOutputStream);
        byte[] bytes = new byte[1];
        when(byteArrayOutputStream.toByteArray()).thenReturn(bytes);

        IOUtils.writeFrames(outputStream, frame1, frame2);

        verify(frame1, times(1)).writeTo(dataOutputStream);
        verify(frame2, times(1)).writeTo(dataOutputStream);
        verify(outputStream, times(1)).write(bytes);
        verify(outputStream, times(1)).flush();
    }
}