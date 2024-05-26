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
import org.apache.skywalking.apm.network.trace.component.command.AsyncProfilerTaskCommand;
import org.apache.skywalking.apm.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AsyncProfilerTask {
    private static final ILog LOGGER = LogManager.getLogger(AsyncProfilerTask.class);
    private static final String OUTPUT_DIR = "skywalking-output";

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
     * maximum Java stack depth (default: 2048)
     */
    private Integer jstackdepth;

    /**
     * profile different threads separately
     */
    private boolean threads;

    /**
     * group threads by scheduling policy
     */
    private boolean sched;

    /**
     * how to collect C stack frames in addition to Java stack
     * MODE is 'fp' (Frame Pointer), 'dwarf', 'lbr' (Last Branch Record) or 'no'
     */
    private String cstack;

    /**
     * use simple class names instead of FQN
     */
    private boolean simple;

    /**
     * print method signatures
     */
    private boolean sig;

    /**
     * annotate Java methods
     */
    private boolean ann;

    /**
     * prepend library names
     */
    private boolean lib;

    /**
     * include only user-mode events
     */
    private boolean alluser;

    /**
     * run profiling for duration seconds
     */
    private Long duration;

    /**
     * include stack traces containing PATTERN
     */
    private List<String> includes;

    /**
     * exclude stack traces containing PATTERN
     */
    private List<String> excludes;

    /**
     * automatically start profiling when the specified native function is executed.
     */
    private String begin;

    /**
     * automatically stop profiling when the specified native function is executed.
     */
    private String end;

    /**
     * time-to-safepoint profiling.
     * An alias for --begin SafepointSynchronize::begin --end RuntimeService::record_safepoint_synchronized
     */
    private boolean ttsp;

    /**
     * FlameGraph title
     */
    private String title;

    /**
     * FlameGraph minimum frame width in percent
     */
    private String minwidth;

    /**
     * generate stack-reversed FlameGraph / Call tree
     */
    private boolean reverse;

    /**
     * count the total value (time, bytes, etc.) instead of samples
     */
    private boolean total;

    /**
     * approximate size of JFR chunk in bytes (default: 100 MB)
     */
    private String chunksize;

    /**
     * duration of JFR chunk in seconds (default: 1 hour)
     */
    private String chunktime;

    /**
     * run profiler in a loop (continuous profiling)
     */
    private String loop;

    /**
     * automatically stop profiler at TIME (absolute or relative)
     */
    private String timeout;

    private String getExecuteArgs() {
        StringBuilder sb = new StringBuilder();
        final char comma = ',';

        // start - start profiling
        // resume - start or resume profiling without resetting collected data
        // stop - stop profiling
        sb.append(action).append(comma);

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
        if (this.jfrsync != null) {
            this.format = "jfr";
            sb.append("jfrsync=").append(this.jfrsync).append(comma);
        }
        if (this.file != null) {
            sb.append("file=").append(this.file).append(comma);
        }
        if (this.format != null) {
            sb.append(this.format).append(comma);
        }
        if (this.interval != null) {
            sb.append("interval=").append(this.interval).append(comma);
        }
        if (this.jstackdepth != null) {
            sb.append("jstackdepth=").append(this.jstackdepth).append(comma);
        }
        if (this.threads) {
            sb.append("threads").append(comma);
        }
        if (this.sched) {
            sb.append("sched").append(comma);
        }
        if (this.cstack != null) {
            sb.append("cstack=").append(this.cstack).append(comma);
        }
        if (this.simple) {
            sb.append("simple").append(comma);
        }
        if (this.sig) {
            sb.append("sig").append(comma);
        }
        if (this.ann) {
            sb.append("ann").append(comma);
        }
        if (this.lib) {
            sb.append("lib").append(comma);
        }
        if (this.alluser) {
            sb.append("alluser").append(comma);
        }
        if (this.includes != null) {
            for (String include : includes) {
                sb.append("include=").append(include).append(comma);
            }
        }
        if (this.excludes != null) {
            for (String exclude : excludes) {
                sb.append("exclude=").append(exclude).append(comma);
            }
        }
        if (this.ttsp) {
            this.begin = "SafepointSynchronize::begin";
            this.end = "RuntimeService::record_safepoint_synchronized";
        }
        if (this.begin != null) {
            sb.append("begin=").append(this.begin).append(comma);
        }
        if (this.end != null) {
            sb.append("end=").append(this.end).append(comma);
        }

        if (this.title != null) {
            sb.append("title=").append(this.title).append(comma);
        }
        if (this.minwidth != null) {
            sb.append("minwidth=").append(this.minwidth).append(comma);
        }
        if (this.reverse) {
            sb.append("reverse").append(comma);
        }
        if (this.total) {
            sb.append("total").append(comma);
        }
        if (this.chunksize != null) {
            sb.append("chunksize=").append(this.chunksize).append(comma);
        }
        if (this.chunktime != null) {
            sb.append("chunktime=").append(this.chunktime).append(comma);
        }
        if (this.loop != null) {
            sb.append("loop=").append(this.loop).append(comma);
        }
        if (this.timeout != null) {
            sb.append("timeout=").append(this.timeout).append(comma);
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

    public String process(AsyncProfiler asyncProfiler) throws IOException {
        if (AsyncProfilerTaskCommand.ProfilerAction.start.equals(action)) {
            String executeArgs = getExecuteArgs();
            return execute(asyncProfiler, executeArgs);
        } else if (AsyncProfilerTaskCommand.ProfilerAction.stop.equals(action)) {
            return processStop(asyncProfiler);
        }

        return "";
    }

    private String processStop(AsyncProfiler asyncProfiler) throws IOException {
        String outputFile = outputFile();
        String executeArgs = getExecuteArgs();
        LOGGER.info("dump async profiler out put file at:{}", outputFile);
        return execute(asyncProfiler, executeArgs);
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
        } else if (this.format.equals("jfr")) {
            fileExt = "jfr";
        } else {
            // illegal -o option makes async-profiler use flat
            fileExt = "txt";
        }
        return fileExt;
    }

    public String getOutputPath() {
        String userDir = System.getProperty("user.dir");
        if (StringUtil.isBlank(userDir)) {
            LOGGER.warn("get user directory from system properties failed");
        }
        return userDir + File.separator + OUTPUT_DIR;
    }

    // TODO create dir when dir is not exist
    private String outputFile() throws IOException {
        if (this.file == null) {
            String fileExt = outputFileExt();
            String outputPath = getOutputPath();
            if (outputPath != null) {
                file = new File(outputPath,
                        new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + "." + fileExt)
                        .getAbsolutePath();
            } else {
                this.file = File.createTempFile(OUTPUT_DIR, "." + fileExt).getAbsolutePath();
            }
        }
        return file;
    }

    public AsyncProfilerTaskCommand.ProfilerAction getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = AsyncProfilerTaskCommand.ProfilerAction.valueOf(action);
    }

    public String getActionArg() {
        return actionArg;
    }

    public void setActionArg(String actionArg) {
        this.actionArg = actionArg;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getAlloc() {
        return alloc;
    }

    public void setAlloc(String alloc) {
        this.alloc = alloc;
    }

    public boolean isLive() {
        return live;
    }

    public void setLive(boolean live) {
        this.live = live;
    }

    public String getLock() {
        return lock;
    }

    public void setLock(String lock) {
        this.lock = lock;
    }

    public String getJfrsync() {
        return jfrsync;
    }

    public void setJfrsync(String jfrsync) {
        this.jfrsync = jfrsync;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public Integer getJstackdepth() {
        return jstackdepth;
    }

    public void setJstackdepth(Integer jstackdepth) {
        this.jstackdepth = jstackdepth;
    }

    public boolean isThreads() {
        return threads;
    }

    public void setThreads(boolean threads) {
        this.threads = threads;
    }

    public boolean isSched() {
        return sched;
    }

    public void setSched(boolean sched) {
        this.sched = sched;
    }

    public String getCstack() {
        return cstack;
    }

    public void setCstack(String cstack) {
        this.cstack = cstack;
    }

    public boolean isSimple() {
        return simple;
    }

    public void setSimple(boolean simple) {
        this.simple = simple;
    }

    public boolean isSig() {
        return sig;
    }

    public void setSig(boolean sig) {
        this.sig = sig;
    }

    public boolean isAnn() {
        return ann;
    }

    public void setAnn(boolean ann) {
        this.ann = ann;
    }

    public boolean isLib() {
        return lib;
    }

    public void setLib(boolean lib) {
        this.lib = lib;
    }

    public boolean isAlluser() {
        return alluser;
    }

    public void setAlluser(boolean alluser) {
        this.alluser = alluser;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    public String getBegin() {
        return begin;
    }

    public void setBegin(String begin) {
        this.begin = begin;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public boolean isTtsp() {
        return ttsp;
    }

    public void setTtsp(boolean ttsp) {
        this.ttsp = ttsp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMinwidth() {
        return minwidth;
    }

    public void setMinwidth(String minwidth) {
        this.minwidth = minwidth;
    }

    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public boolean isTotal() {
        return total;
    }

    public void setTotal(boolean total) {
        this.total = total;
    }

    public String getChunksize() {
        return chunksize;
    }

    public void setChunksize(String chunksize) {
        this.chunksize = chunksize;
    }

    public String getChunktime() {
        return chunktime;
    }

    public void setChunktime(String chunktime) {
        this.chunktime = chunktime;
    }

    public String getLoop() {
        return loop;
    }

    public void setLoop(String loop) {
        this.loop = loop;
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }
}
