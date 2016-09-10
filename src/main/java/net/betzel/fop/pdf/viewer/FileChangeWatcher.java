/*
 * Copyright 2016 betzel.net.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.betzel.fop.pdf.viewer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author mbetzel
 */
public class FileChangeWatcher {

    private final String name;
    private final Set<Path> targets = new HashSet<>();
    private final Map<Path, FileTime> modifiedTimes = new HashMap<>();
    private final long pollingTimeMillis;
    private final FileChange delegate;
    private volatile boolean started;
    private Timer watchingTimer;

    public FileChangeWatcher(long pollingTimeMillis, FileChange delegate, String name) {
        this.pollingTimeMillis = pollingTimeMillis;
        this.delegate = delegate;
        this.name = getClass().getSimpleName() + "[" + name + "]";
    }

    public synchronized void addTarget(Path target) {
        if (target != null && !targets.contains(target)) {
            targets.add(target);
            try {
                FileTime modifiedTime = Files.getLastModifiedTime(target);
                modifiedTimes.put(target, modifiedTime);
            } catch (IOException ignored) {
            }
            updateWatchingTimer();
        }
    }

    public synchronized void removeTarget(Path target) {
        if (target != null && !targets.contains(target)) {
            targets.remove(target);
            modifiedTimes.remove(target);
            updateWatchingTimer();
        }
    }

    public synchronized void setTargets(Collection<Path> newTargets) {
        Set<Path> toBeAdded = new HashSet<>();
        toBeAdded.addAll(newTargets);
        toBeAdded.removeAll(targets);
        Set<Path> toBeRemoved = new HashSet<>();
        toBeRemoved.addAll(targets);
        toBeRemoved.removeAll(newTargets);
        for (Path target : toBeAdded) {
            addTarget(target);
        }
        for (Path target : toBeRemoved) {
            removeTarget(target);
        }
    }

    public synchronized Set<Path> getTargets() {
        return Collections.unmodifiableSet(targets);
    }

    public synchronized void start() {
        if (!isStarted()) {
            started = true;
            updateWatchingTimer();
        }
    }

    public synchronized void stop() {
        if (isStarted()) {
            started = false;
            updateWatchingTimer();
        }
    }

    public synchronized boolean isStarted() {
        return started;
    }

    public static interface FileChange {

        public void changed();
    }

    private void updateWatchingTimer() {
        // deamon thread
        final boolean timerNeeded = started && (targets.isEmpty() == false);

        if (timerNeeded) {
            if (watchingTimer == null) {
                watchingTimer = new Timer(name, true);
                watchingTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runWatching();
                    }
                }, pollingTimeMillis, pollingTimeMillis);
            }
        } else if (watchingTimer != null) {
            watchingTimer.cancel();
            watchingTimer = null;
        }
    }

    private synchronized void runWatching() {
        if (watchingTimer != null) {
            for (Path target : targets) {
                FileTime newModifiedTime;
                try {
                    newModifiedTime = Files.getLastModifiedTime(target);
                } catch (IOException x) {
                    newModifiedTime = null;
                }
                FileTime lastModifiedTime = modifiedTimes.get(target);
                if ((lastModifiedTime == null) && (newModifiedTime != null)) {
                    // target has been created
                } else if ((lastModifiedTime != null) && (newModifiedTime == null)) {
                    // target has been deleted
                } else if (Objects.equals(lastModifiedTime, newModifiedTime) == false) {
                    // target has been modified
                    if (newModifiedTime != null) {
                        modifiedTimes.put(target, newModifiedTime);
                        delegate.changed();
                    }
                }
            }
        }
    }

}