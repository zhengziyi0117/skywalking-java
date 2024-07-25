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

package org.apache.skywalking.apm.agent.core.commands.executor;

import org.apache.skywalking.apm.agent.core.asyncprofiler.AsyncProfilerTask;
import org.apache.skywalking.apm.agent.core.asyncprofiler.AsyncProfilerTaskExecutionService;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.commands.CommandExecutionException;
import org.apache.skywalking.apm.agent.core.commands.CommandExecutor;
import org.apache.skywalking.apm.network.trace.component.command.AsyncProfilerTaskCommand;
import org.apache.skywalking.apm.network.trace.component.command.BaseCommand;

public class AsyncProfilerCommandExecutor implements CommandExecutor {
    @Override
    public void execute(BaseCommand command) throws CommandExecutionException {
        AsyncProfilerTaskCommand asyncProfilerTaskCommand = (AsyncProfilerTaskCommand) command;

        AsyncProfilerTask asyncProfilerTask = new AsyncProfilerTask();
        asyncProfilerTask.setTaskId(asyncProfilerTaskCommand.getTaskId());
        asyncProfilerTask.setDuration(asyncProfilerTaskCommand.getDuration());
        asyncProfilerTask.setExecArgs(asyncProfilerTaskCommand.getExecArgs());
        asyncProfilerTask.setCreateTime(asyncProfilerTaskCommand.getCreateTime());
        asyncProfilerTask.setDataFormat(asyncProfilerTaskCommand.getDataFormat());
        ServiceManager.INSTANCE.findService(AsyncProfilerTaskExecutionService.class)
                .processAsyncProfilerTask(asyncProfilerTask);
    }
}
