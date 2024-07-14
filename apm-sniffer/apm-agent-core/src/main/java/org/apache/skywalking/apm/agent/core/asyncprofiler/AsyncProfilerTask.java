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
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.network.language.asyncprofile.v3.AsyncProfilerDataFormatType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AsyncProfilerTask {
    private static final ILog LOGGER = LogManager.getLogger(AsyncProfilerTask.class);
    /**
     * task id
     */
    private String taskId;
    /**
     * execArgument from oap server
     */
    private String execArgs;
    /**
     * run profiling for duration seconds
     */
    private int duration;
    /**
     * run profiling for duration seconds
     */
    private long createTime;
    /**
     * temp File
     */
    private Path tempFile;

    private AsyncProfilerDataFormatType dataFormat;

    private static String execute(AsyncProfiler asyncProfiler, String arg)
            throws IllegalArgumentException, IOException {
        LOGGER.info("async profiler execute arg:{}", arg);
        String result = asyncProfiler.execute(arg);
        return result.trim();
    }

    public String start(AsyncProfiler asyncProfiler) throws IOException {

        tempFile = Files.createFile(Paths.get("/Users/bytedance/IdeaProjects/skywalking-java/skywalking-output/" + taskId, ""));
        execArgs = execArgs + "file=" + tempFile.toAbsolutePath();
        return execute(asyncProfiler, execArgs);
    }

    /**
     * stop async-profiler and dump profile data
     */
    public InputStream stop(AsyncProfiler asyncProfiler) throws IOException {
        LOGGER.info("async profiler process stop and dump file");
        asyncProfiler.stop();
//        return dumpJFR();
        if (AsyncProfilerDataFormatType.JFR.equals(dataFormat)) {
            return getJFRInputStream();
        } else if (AsyncProfilerDataFormatType.HTML.equals(dataFormat)) {
            return null;
        } else {
            return null;
        }
    }

    private String getFileExtension() {
        switch (dataFormat) {
            case JFR:
                return ".jfr";
            case HTML:
                return ".html";
            default:
                return ".html";
        }
    }

    /**
     * need manually close
     */
    private InputStream getJFRInputStream() throws IOException {
        File file = tempFile.toFile();
        file.deleteOnExit();
        return Files.newInputStream(file.toPath());
    }

    public void setExecArgs(String execArgs) {
        this.execArgs = execArgs;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setTempFile(Path tempFile) {
        this.tempFile = tempFile;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public void setDataFormat(AsyncProfilerDataFormatType dataFormat) {
        this.dataFormat = dataFormat;
    }

    public AsyncProfilerDataFormatType getDataFormat() {
        return this.dataFormat;
    }

    public String getExecArgs() {
        return execArgs;
    }

    public int getDuration() {
        return duration;
    }

    public Path getTempFile() {
        return tempFile;
    }

    public String getTaskId() {
        return taskId;
    }

    public long getCreateTime() {
        return createTime;
    }
}
