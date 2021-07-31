/*
 * Copyright 2021 MixtapeBot and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sedmelluq.discord.lavaplayer.udpqueue.natives;


import com.sedmelluq.lava.common.natives.NativeLibraryLoader;

import java.nio.ByteBuffer;

public class UdpQueueManagerLibrary {
    private static final NativeLibraryLoader nativeLoader =
        NativeLibraryLoader.create(UdpQueueManagerLibrary.class, "udpqueue");

    private UdpQueueManagerLibrary() {

    }

    public static UdpQueueManagerLibrary getInstance() {
        nativeLoader.load();
        return new UdpQueueManagerLibrary();
    }

    public native long create(int bufferCapacity, long packetInterval);

    public native void destroy(long instance);

    public native int getRemainingCapacity(long instance, long key);

    public native boolean queuePacket(long instance, long key, String address, int port, ByteBuffer dataDirectBuffer,
                                      int dataLength);

    public native boolean queuePacketWithSocket(long instance, long key, String address, int port,
                                                ByteBuffer dataDirectBuffer, int dataLength, long explicitSocket);

    public native boolean deleteQueue(long instance, long key);

    public native void process(long instance);

    public native void processWithSocket(long instance, long ipv4Handle, long ipv6Handle);

    public static native void pauseDemo(int length);
}
