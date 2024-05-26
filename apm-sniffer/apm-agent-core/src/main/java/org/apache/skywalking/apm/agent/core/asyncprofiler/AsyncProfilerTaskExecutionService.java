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

import io.pyroscope.one.profiler.AsyncProfiler;
import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.DefaultImplementor;
import org.apache.skywalking.apm.agent.core.boot.DefaultNamedThreadFactory;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DefaultImplementor
public class AsyncProfilerTaskExecutionService implements BootService {

    private static final ILog LOGGER = LogManager.getLogger(AsyncProfilerTaskChannelService.class);

    //TODO use local so file for debug
    private static final AsyncProfiler ASYNC_PROFILER = AsyncProfiler.getInstance("/opt/async-profiler/lib/libasyncProfiler.so");

    // profile executor thread pool, only running one thread
    private final static ExecutorService ASYNC_PROFILE_EXECUTOR = Executors.newSingleThreadExecutor(
            new DefaultNamedThreadFactory("ASYNC-PROFILING-TASK"));

    public void processAsyncProfilerTask(AsyncProfilerTask task) {
        // add task to list
        LOGGER.info("add async profiler task: {}", task);
//        asyncProfileTaskList.add(task);
        ASYNC_PROFILE_EXECUTOR.execute(() -> {
            try {
                task.process(ASYNC_PROFILER);
            } catch (IOException e) {
                LOGGER.error("AsyncProfilerTask executor error:" + e.getMessage(), e);
            }
        });
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
