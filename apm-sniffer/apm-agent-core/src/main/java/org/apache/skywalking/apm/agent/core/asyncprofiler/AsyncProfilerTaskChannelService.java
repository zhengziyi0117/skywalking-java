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

import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.DefaultImplementor;
import org.apache.skywalking.apm.agent.core.boot.DefaultNamedThreadFactory;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.commands.CommandService;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.remote.GRPCChannelStatus;
import org.apache.skywalking.apm.network.common.v3.Command;
import org.apache.skywalking.apm.network.common.v3.Commands;
import org.apache.skywalking.apm.network.common.v3.KeyStringValuePair;
import org.apache.skywalking.apm.network.trace.component.command.AsyncProfilerTaskCommand;
import org.apache.skywalking.apm.util.RunnableWithExceptionProtection;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * TODO <p>add receive grpc command<p/>
 * now just debug AsyncProfilerTask
 */
@DefaultImplementor
public class AsyncProfilerTaskChannelService implements BootService, Runnable {
    private static final ILog LOGGER = LogManager.getLogger(AsyncProfilerTaskChannelService.class);

    // channel status
    private volatile GRPCChannelStatus status = GRPCChannelStatus.DISCONNECT;

    // query task list schedule
    private volatile ScheduledFuture<?> getTaskListFuture;

    @Override
    public void run() {
        if (status == GRPCChannelStatus.CONNECTED) {
            // test start command and 10s after put stop command
            Command startCommand = Command.newBuilder()
                    .addArgs(KeyStringValuePair.newBuilder().setKey("Action").setValue("start"))
                    .addArgs(KeyStringValuePair.newBuilder().setKey("SerialNumber").setValue("1"))
                    .setCommand(AsyncProfilerTaskCommand.NAME)
                    .build();
            Commands startCommands = Commands.newBuilder()
                    .addCommands(startCommand)
                    .build();
            ServiceManager.INSTANCE.findService(CommandService.class).receiveCommand(startCommands);

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Command stopCommand = Command.newBuilder()
                    .addArgs(KeyStringValuePair.newBuilder().setKey("Action").setValue("stop"))
                    .addArgs(KeyStringValuePair.newBuilder().setKey("SerialNumber").setValue("2"))
                    .setCommand(AsyncProfilerTaskCommand.NAME)
                    .build();
            Commands stopCommands = Commands.newBuilder()
                    .addCommands(stopCommand)
                    .build();
            ServiceManager.INSTANCE.findService(CommandService.class).receiveCommand(stopCommands);

            status = GRPCChannelStatus.DISCONNECT;
        }
    }

    @Override
    public void prepare() throws Throwable {
        status = GRPCChannelStatus.CONNECTED;
    }

    @Override
    public void boot() throws Throwable {
        getTaskListFuture = Executors.newSingleThreadScheduledExecutor(
                new DefaultNamedThreadFactory("AsyncProfileGetTaskService")
        ).scheduleWithFixedDelay(
                new RunnableWithExceptionProtection(
                        this,
                        t -> LOGGER.error("Query async profile task list failure.", t)
                ), 0, Config.Collector.GET_PROFILE_TASK_INTERVAL, TimeUnit.SECONDS
        );
    }

    @Override
    public void onComplete() throws Throwable {

    }

    @Override
    public void shutdown() throws Throwable {
        if (getTaskListFuture != null) {
            getTaskListFuture.cancel(true);
        }
    }
}
