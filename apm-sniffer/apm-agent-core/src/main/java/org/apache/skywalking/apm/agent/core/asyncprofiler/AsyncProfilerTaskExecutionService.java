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

import io.pyroscope.labels.io.pyroscope.PyroscopeAsyncProfiler;
import io.pyroscope.one.profiler.AsyncProfiler;
import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.DefaultImplementor;
import org.apache.skywalking.apm.agent.core.boot.DefaultNamedThreadFactory;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.util.StringUtil;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@DefaultImplementor
public class AsyncProfilerTaskExecutionService implements BootService {

    private static final ILog LOGGER = LogManager.getLogger(AsyncProfilerTaskChannelService.class);

    private static final AsyncProfiler ASYNC_PROFILER = PyroscopeAsyncProfiler.getAsyncProfiler();

    // profile executor thread pool, only running one thread
    private final static ScheduledExecutorService ASYNC_PROFILE_EXECUTOR = Executors.newSingleThreadScheduledExecutor(
            new DefaultNamedThreadFactory("ASYNC-PROFILING-TASK"));

    private volatile ProfileState status = ProfileState.STOP;

    private enum ProfileState {
        PROFILING,
        STOP;
    }

    public void processAsyncProfilerTask(AsyncProfilerTask task) {
        // add task to list
        LOGGER.info("add async profiler task: {}", task);
//        asyncProfileTaskList.add(task);
        ASYNC_PROFILE_EXECUTOR.execute(() -> {
            try {
                if (status == ProfileState.PROFILING) {
                    // stop pre task
                    status = ProfileState.STOP;
                    // todo stop pre task
                }
                if (Objects.isNull(task.getDuration())) {
                    LOGGER.error("async profile task must need duration");
                }
                String result = task.process(ASYNC_PROFILER);

                LOGGER.info("AsyncProfilerTask executor result:{}", result);
//                PROFILE_TASK_SCHEDULE.schedule(() -> processProfileTask(task), timeToProcessMills, TimeUnit.MILLISECONDS);
                ASYNC_PROFILE_EXECUTOR.schedule(() -> stopAsyncProfile(task), task.getDuration(), TimeUnit.MICROSECONDS);
            } catch (IOException e) {
                LOGGER.error("AsyncProfilerTask executor error:" + e.getMessage(), e);
            }
        });
    }

    private void stopAsyncProfile(AsyncProfilerTask task) {
        // execute stop task

        status = ProfileState.STOP;
        // upload file

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
        ASYNC_PROFILE_EXECUTOR.shutdown();
    }
}
