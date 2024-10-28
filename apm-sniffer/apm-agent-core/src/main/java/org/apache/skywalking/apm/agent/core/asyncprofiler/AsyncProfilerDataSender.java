/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.agent.core.asyncprofiler;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.DefaultImplementor;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.profile.ProfileSnapshotSender;
import org.apache.skywalking.apm.agent.core.remote.GRPCChannelListener;
import org.apache.skywalking.apm.agent.core.remote.GRPCChannelManager;
import org.apache.skywalking.apm.agent.core.remote.GRPCChannelStatus;
import org.apache.skywalking.apm.agent.core.remote.GRPCStreamServiceStatus;
import org.apache.skywalking.apm.network.common.v3.Commands;
import org.apache.skywalking.apm.network.language.asyncprofiler.v10.AsyncProfilerCollectType;
import org.apache.skywalking.apm.network.language.asyncprofiler.v10.AsyncProfilerData;
import org.apache.skywalking.apm.network.language.asyncprofiler.v10.AsyncProfilerMetaData;
import org.apache.skywalking.apm.network.language.asyncprofiler.v10.AsyncProfilerTaskGrpc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.apache.skywalking.apm.agent.core.conf.Config.Collector.GRPC_UPSTREAM_TIMEOUT;

@DefaultImplementor
public class AsyncProfilerDataSender implements BootService, GRPCChannelListener {
    private static final ILog LOGGER = LogManager.getLogger(ProfileSnapshotSender.class);
    private static final int DATA_CHUNK_SIZE = 1024 * 1024;

    private volatile GRPCChannelStatus status = GRPCChannelStatus.DISCONNECT;

    private volatile AsyncProfilerTaskGrpc.AsyncProfilerTaskStub asyncProfilerTaskStub;

    @Override
    public void prepare() throws Throwable {
        ServiceManager.INSTANCE.findService(GRPCChannelManager.class).addChannelListener(this);
    }

    @Override
    public void boot() throws Throwable {

    }

    @Override
    public void onComplete() throws Throwable {

    }

    @Override
    public void shutdown() throws Throwable {

    }

    @Override
    public void statusChanged(GRPCChannelStatus status) {
        if (GRPCChannelStatus.CONNECTED.equals(status)) {
            Channel channel = ServiceManager.INSTANCE.findService(GRPCChannelManager.class).getChannel();
            asyncProfilerTaskStub = AsyncProfilerTaskGrpc.newStub(channel);
        } else {
            asyncProfilerTaskStub = null;
        }
        this.status = status;
    }

    public void sendData(AsyncProfilerTask task, FileChannel channel) throws IOException, InterruptedException {
        if (status != GRPCChannelStatus.CONNECTED || Objects.isNull(channel) || !channel.isOpen()) {
            return;
        }

        int size = Math.toIntExact(channel.size());
        boolean[] isAbort = new boolean[]{false};
        final GRPCStreamServiceStatus status = new GRPCStreamServiceStatus(false);
        StreamObserver<AsyncProfilerData> dataStreamObserver = asyncProfilerTaskStub.withDeadlineAfter(
                GRPC_UPSTREAM_TIMEOUT, TimeUnit.SECONDS
        ).collect(new StreamObserver<Commands>() {
            @Override
            public void onNext(Commands value) {

            }

            @Override
            public void onError(Throwable t) {
                isAbort[0] = true;
                status.finished();
                if (LOGGER.isErrorEnable()) {
                    LOGGER.error(
                            t, "Send async profiler task data to collector fail with a grpc internal exception."
                    );
                }
                ServiceManager.INSTANCE.findService(GRPCChannelManager.class).reportError(t);
            }

            @Override
            public void onCompleted() {
                status.finished();
            }
        });
        AsyncProfilerMetaData metaData = AsyncProfilerMetaData.newBuilder()
                .setService(Config.Agent.SERVICE_NAME)
                .setServiceInstance(Config.Agent.INSTANCE_NAME)
                .setType(AsyncProfilerCollectType.PROFILING_SUCCESS)
                .setContentSize(size)
                .setTaskId(task.getTaskId())
                .build();
        AsyncProfilerData asyncProfilerData = AsyncProfilerData.newBuilder().setMetaData(metaData).build();
        dataStreamObserver.onNext(asyncProfilerData);
        /**
         * Wait briefly to see if the server can receive the jfr. If the server cannot receive it, onError will be triggered.
         * Then we wait for a while (waiting for the server to send onError) and then decide whether to send the jfr file.
         */
        Thread.sleep(500);

        // Is it possible to upload jfr?
        ByteBuffer buf = ByteBuffer.allocateDirect(DATA_CHUNK_SIZE);
        while (isAbort[0] && channel.read(buf) > 0) {
            buf.flip();
            asyncProfilerData = AsyncProfilerData.newBuilder()
                    .setContent(ByteString.copyFrom(buf))
                    .build();
            dataStreamObserver.onNext(asyncProfilerData);
            buf.clear();
        }
        dataStreamObserver.onCompleted();

        status.wait4Finish();
    }

    public void sendError(AsyncProfilerTask task, String errorMessage) {
        if (status != GRPCChannelStatus.CONNECTED) {
            return;
        }
        final GRPCStreamServiceStatus status = new GRPCStreamServiceStatus(false);
        StreamObserver<AsyncProfilerData> dataStreamObserver = asyncProfilerTaskStub.withDeadlineAfter(
                GRPC_UPSTREAM_TIMEOUT, TimeUnit.SECONDS
        ).collect(new StreamObserver<Commands>() {
            @Override
            public void onNext(Commands value) {
            }

            @Override
            public void onError(Throwable t) {
                status.finished();
                ServiceManager.INSTANCE.findService(GRPCChannelManager.class).reportError(t);
            }

            @Override
            public void onCompleted() {
                status.finished();
            }
        });
        AsyncProfilerMetaData metaData = AsyncProfilerMetaData.newBuilder()
                .setService(Config.Agent.SERVICE_NAME)
                .setServiceInstance(Config.Agent.INSTANCE_NAME)
                .setTaskId(task.getTaskId())
                .setType(AsyncProfilerCollectType.EXECUTION_TASK_ERROR)
                .setContentSize(0)
                .build();
        AsyncProfilerData asyncProfilerData = AsyncProfilerData.newBuilder()
                .setMetaData(metaData)
                .setErrorMessage(errorMessage)
                .build();
        dataStreamObserver.onNext(asyncProfilerData);
        dataStreamObserver.onCompleted();
        status.wait4Finish();
    }
}