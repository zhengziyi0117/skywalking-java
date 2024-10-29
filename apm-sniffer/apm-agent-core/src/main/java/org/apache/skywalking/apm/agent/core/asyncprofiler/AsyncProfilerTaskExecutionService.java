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

import one.profiler.AsyncProfiler;
import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.DefaultImplementor;
import org.apache.skywalking.apm.agent.core.boot.DefaultNamedThreadFactory;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@DefaultImplementor
public class AsyncProfilerTaskExecutionService implements BootService {

    private static final ILog LOGGER = LogManager.getLogger(AsyncProfilerTaskChannelService.class);

    private static final AsyncProfiler ASYNC_PROFILER = AsyncProfiler.getInstance();

    private static final String SUCCESS_RESULT = "Profiling started";

    // profile executor thread pool, only running one thread
    private static final ScheduledExecutorService ASYNC_PROFILER_EXECUTOR = Executors.newSingleThreadScheduledExecutor(
            new DefaultNamedThreadFactory("ASYNC-PROFILING-TASK"));

    // last command create time, use to next query task list
    private volatile long lastCommandCreateTime = -1;

    // task schedule future
    private volatile ScheduledFuture<?> scheduledFuture;

    public void processAsyncProfilerTask(AsyncProfilerTask task) {
        if (task.getCreateTime() <= lastCommandCreateTime) {
            LOGGER.warn("get repeat task because createTime is less than lastCommandCreateTime");
            return;
        }
        lastCommandCreateTime = task.getCreateTime();
        LOGGER.info("add async profiler task: {}", task.getTaskId());
        // add task to list
        ASYNC_PROFILER_EXECUTOR.execute(() -> {
            try {
                if (Objects.nonNull(scheduledFuture) && !scheduledFuture.isDone()) {
                    LOGGER.info("AsyncProfilerTask already running");
                    return;
                }

                String result = task.start(ASYNC_PROFILER);
                if (!SUCCESS_RESULT.equals(result)) {
                    stopWhenError(task, result);
                    return;
                }
                scheduledFuture = ASYNC_PROFILER_EXECUTOR.schedule(
                        () -> stopWhenSuccess(task), task.getDuration(), TimeUnit.SECONDS
                );
            } catch (IOException e) {
                LOGGER.error("AsyncProfilerTask executor error:" + e.getMessage(), e);
            }
        });
    }

    private void stopWhenError(AsyncProfilerTask task, String errorMessage) {
        LOGGER.error("AsyncProfilerTask start fail result:" + errorMessage);
        AsyncProfilerDataSender dataSender = ServiceManager.INSTANCE.findService(AsyncProfilerDataSender.class);
        dataSender.sendError(task, errorMessage);
    }

    private void stopWhenSuccess(AsyncProfilerTask task) {

        try {
            File dumpFile = task.stop(ASYNC_PROFILER);
            // stop task
            try (FileInputStream fileInputStream = new FileInputStream(dumpFile)) {
                // upload file
                FileChannel channel = fileInputStream.getChannel();

                AsyncProfilerDataSender dataSender = ServiceManager.INSTANCE.findService(AsyncProfilerDataSender.class);
                dataSender.sendData(task, channel);
            }

            if (!dumpFile.delete()) {
                LOGGER.warn("delete async profiler dump file failed");
            }
        } catch (Exception e) {
            LOGGER.error("stop async profiler task error", e);
            return;
        }
    }

    public long getLastCommandCreateTime() {
        return lastCommandCreateTime;
    }

    @Override
    public void prepare() throws Throwable {

    }

    @Override
    public void boot() throws Throwable {

    }

    @Override
    public void onComplete() throws Throwable {

    }

    @Override
    public void shutdown() throws Throwable {
        scheduledFuture.cancel(true);
        ASYNC_PROFILER_EXECUTOR.shutdown();
        scheduledFuture = null;
    }
}
