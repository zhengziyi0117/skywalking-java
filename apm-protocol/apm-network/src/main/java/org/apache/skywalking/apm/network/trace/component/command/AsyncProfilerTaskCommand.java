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

package org.apache.skywalking.apm.network.trace.component.command;

import org.apache.skywalking.apm.network.common.v3.Command;
import org.apache.skywalking.apm.network.common.v3.KeyStringValuePair;

import java.util.List;

/**
 * TODO add full arg
 */
public class AsyncProfilerTaskCommand extends BaseCommand implements Serializable, Deserializable<AsyncProfilerTaskCommand> {
    public static final Deserializable<AsyncProfilerTaskCommand> DESERIALIZER = new AsyncProfilerTaskCommand("", "");
    public static final String NAME = "AsyncProfilerQuery";

    private String action;
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

    public enum ProfilerAction {
        // start, resume, stop, dump, check, status, meminfo, list, collect,
        start, resume, stop, dump, check, status, meminfo, list, collect,
        version,
    }

    public AsyncProfilerTaskCommand(String command, String serialNumber) {
        super(command, serialNumber);
    }

    @Override
    public Command.Builder serialize() {
        final Command.Builder builder = commandBuilder();
        builder.addArgs(KeyStringValuePair.newBuilder().setKey("Action").setValue(action))
                .addArgs(KeyStringValuePair.newBuilder().setKey("ActionArg").setValue(actionArg))
                .addArgs(KeyStringValuePair.newBuilder().setKey("Event").setValue(event))
                .addArgs(KeyStringValuePair.newBuilder().setKey("Alloc").setValue(alloc));
        return builder;
    }

    @Override
    public AsyncProfilerTaskCommand deserialize(Command command) {
        List<KeyStringValuePair> args = command.getArgsList();

        String serialNumber = null;
        String action = null;
        String actionArg = null;
        String event = null;
        String alloc = null;

        for (final KeyStringValuePair pair : args) {
            if ("SerialNumber".equals(pair.getKey())) {
                serialNumber = pair.getValue();
            } else if ("Action".equals(pair.getKey())) {
                action = pair.getValue();
            } else if ("ActionArg".equals(pair.getKey())) {
                actionArg = pair.getValue();
            } else if ("Event".equals(pair.getKey())) {
                event = pair.getValue();
            } else if ("Alloc".equals(pair.getKey())) {
                alloc = pair.getValue();
            }
        }

        AsyncProfilerTaskCommand asyncProfilerTaskCommand = new AsyncProfilerTaskCommand(NAME, serialNumber);
        asyncProfilerTaskCommand.setAction(action);
        asyncProfilerTaskCommand.setActionArg(actionArg);
        asyncProfilerTaskCommand.setEvent(event);
        asyncProfilerTaskCommand.setAlloc(alloc);
        return asyncProfilerTaskCommand;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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
