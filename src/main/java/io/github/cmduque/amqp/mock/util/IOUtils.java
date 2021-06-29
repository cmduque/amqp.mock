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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IOUtils {
    private static final String RECEIVED = "received --> {}";
    private static final String SENT = "sent --> {}";

    public static Method readMethod(DataInputStream dataInputStream) throws IOException {
        Method method = AMQImpl.readMethodFrom(dataInputStream);
        log.debug(RECEIVED, method);
        return method;
    }

    public static AMQP.BasicProperties readContentHeader(DataInputStream dataInputStream) throws IOException {
        Frame contentHeaderFrame = Frame.readFrom(dataInputStream);
        AMQP.BasicProperties contentHeader = (AMQP.BasicProperties) AMQImpl.readContentHeaderFrom(contentHeaderFrame.getInputStream());
        log.debug(RECEIVED, contentHeader);
        return contentHeader;
    }

    public static void writeMethod(OutputStream outputStream, int channelNumber, Method method) throws IOException {
        Frame frame = method.toFrame(channelNumber);
        log.debug(SENT, method);
        writeFrames(outputStream, frame);
    }

    public static void writeFrames(OutputStream outputStream, Frame... frames) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        Stream.of(frames).forEach(frame -> {
            try {
                frame.writeTo(dataOutputStream);
            } catch (IOException ex) {
                log.error("An error occurs writing frame in output", ex);
            }
        });

        byte[] bytes = byteArrayOutputStream.toByteArray();
        logBytes(bytes);
        outputStream.write(bytes);
        outputStream.flush();
    }

    public static void logBytes(byte[] bytes) {
        String hexString = IntStream.range(0, bytes.length).parallel()
                .mapToObj(idx -> Integer.toHexString(bytes[idx])).collect(Collectors.joining(" "));
        log.trace(hexString);
    }
}