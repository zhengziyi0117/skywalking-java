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
import io.pyroscope.one.profiler.Counter;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.network.trace.component.command.AsyncProfilerTaskCommand;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AsyncProfilerTask {
    private static final ILog LOGGER = LogManager.getLogger(AsyncProfilerTask.class);
    private static final String JFR = "jfr";

    private AsyncProfilerTaskCommand.ProfilerAction action;
    private String actionArg;

    /**
     * which event to trace (cpu, wall, cache-misses, etc.)
     */
    private String event;

    /**
     * profile allocations with BYTES interval
     * according to async-profiler README, alloc may contains non-numeric characters
     */
    private String alloc;

    /**
     * build allocation profile from live objects only
     */
    private boolean live;

    /**
     * profile contended locks longer than DURATION ns
     * according to async-profiler README, alloc may contains non-numeric characters
     */
    private String lock;

    /**
     * start Java Flight Recording with the given config along with the profiler
     */
    private String jfrsync;

    /**
     * output file name for dumping
     */
    private String file;

    /**
     * output file format, default value is html.
     */
    private String format;

    /**
     * sampling interval in ns (default: 10'000'000, i.e. 10 ms)
     */
    private Long interval;

    /**
     * run profiling for duration seconds
     */
    private Long duration;

    private File tempJFRFile;

    private String getExecuteArgs() throws IOException {
        StringBuilder sb = new StringBuilder();
        final char comma = ',';

        sb.append("start");

        if (this.event != null) {
            sb.append("event=").append(this.event).append(comma);
        }
        if (this.alloc != null) {
            sb.append("alloc=").append(this.alloc).append(comma);
        }
        if (this.live) {
            sb.append("live").append(comma);
        }
        if (this.lock != null) {
            sb.append("lock=").append(this.lock).append(comma);
        }
        if (this.interval != null) {
            sb.append("interval=").append(this.interval).append(comma);
        }
        if (this.format != null) {
            sb.append(this.format).append(comma);
            if (JFR.equals(format)) {
                // flight recorder is built on top of a file descriptor, so we need a file.
                tempJFRFile = File.createTempFile("skywalking", ".jfr");
                tempJFRFile.deleteOnExit();
            }
        }
        if (this.file != null) {
            sb.append("file=").append(this.file).append(comma);
        }
        return sb.toString();
    }

    private static String execute(AsyncProfiler asyncProfiler, String arg)
            throws IllegalArgumentException, IOException {
        LOGGER.info("async profiler execute arg:{}", arg);
        String result = asyncProfiler.execute(arg);
        if (!result.endsWith("\n")) {
            result += "\n";
        }
        return result;
    }

    private String start(AsyncProfiler asyncProfiler) throws IOException {
        String executeArgs = getExecuteArgs();
        return execute(asyncProfiler, executeArgs);
    }

    /**
     * stop async-profiler and dump profile data
     */
    private byte[] processStop(AsyncProfiler asyncProfiler) throws IOException {
        asyncProfiler.stop();
        final byte[] data;
        if (format.equals(JFR)) {
            data = dumpJFR();
        } else {
            data = asyncProfiler.dumpCollapsed(Counter.SAMPLES).getBytes(StandardCharsets.UTF_8);
        }
        return data;
    }

    private byte[] dumpJFR() {
        try {
            byte[] bytes = new byte[(int) tempJFRFile.length()];
            try (DataInputStream ds = new DataInputStream(new FileInputStream(tempJFRFile))) {
                ds.readFully(bytes);
            }
            return bytes;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * This method should only be called when {@code this.file == null} is true.
     */
    private String outputFileExt() {
        String fileExt = "";
        if (this.format == null) {
            fileExt = "html";
        } else if (this.format.startsWith("flat") || this.format.startsWith("traces")
                || this.format.equals("collapsed")) {
            fileExt = "txt";
        } else if (this.format.equals("flamegraph") || this.format.equals("tree")) {
            fileExt = "html";
        } else if (this.format.equals(JFR)) {
            fileExt = JFR;
        } else {
            // illegal -o option makes async-profiler use flat
            fileExt = "txt";
        }
        return fileExt;
    }

}
