/*
 *    Copyright 2013-2014 UOL - Universo Online Team
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package br.com.uol.runas.classloader;

import java.beans.Introspector;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.LogFactory;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.stereotype.Component;

import br.com.uol.runas.service.helper.Reflections;

@Component
public class ClassLoaderGC implements Runnable {

    private final Queue<WeakReference<ClassLoader>> classLoaders;
    private final ExecutorService executor;

    public ClassLoaderGC() {
        this.classLoaders = new ConcurrentLinkedQueue<>();
        this.executor = Executors.newSingleThreadExecutor();
    }

    @PostConstruct
    public void start() {
        executor.submit(this);
    }

    @PreDestroy
    public void stop() {
        classLoaders.clear();
        executor.shutdownNow();
    }

    public void collect(ClassLoader classLoader) {
        classLoaders.add(new WeakReference<ClassLoader>(classLoader));
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            gc(classLoaders.poll());
            sleep();
        }
    }

    private void gc(WeakReference<ClassLoader> classLoader) {
        if (classLoader == null || classLoader.get() == null) {
            return;
        }

        release(classLoader);
        forceGC(classLoader);
    }

    private void release(WeakReference<ClassLoader> classLoader) {
        releaseFromLoggers(classLoader);
        releaseFromBeans(classLoader);
        releaseFromShutdownHooks(classLoader);
        releaseFromThreads(classLoader);
    }

    @SuppressWarnings("unchecked")
    private void releaseFromShutdownHooks(WeakReference<ClassLoader> classLoader) {
        final Map<Thread, Thread> hooks = (Map<Thread, Thread>) Reflections.getStaticFieldValue("java.lang.ApplicationShutdownHooks", "hooks");

        if(hooks != null) {
            final List<Thread> shutdownHooks = new ArrayList<>(hooks.keySet());

            for(Thread shutdownHook : shutdownHooks) {
                if (Objects.equals(classLoader.get(), shutdownHook.getContextClassLoader())) {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                }
            }
        }
    }

    private void releaseFromThreads(WeakReference<ClassLoader> classLoader) {
        final List<Thread> threads = new ArrayList<Thread>(Thread.getAllStackTraces().keySet());

        for (Thread thread : threads) {
            if (Objects.equals(classLoader.get(), thread.getContextClassLoader())) {
                thread.setContextClassLoader(null);
            }
        }
    }

    private void releaseFromBeans(WeakReference<ClassLoader> classLoader) {
        CachedIntrospectionResults.clearClassLoader(classLoader.get());
        Introspector.flushCaches();
    }

    private void releaseFromLoggers(WeakReference<ClassLoader> classLoader) {
        LogFactory.release(classLoader.get());
    }

    private void forceGC(WeakReference<?> reference) {
        while (reference.get() != null && !Thread.currentThread().isInterrupted()) {
            System.gc();
            sleep();
        }
    }

    private void sleep() {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        } catch (InterruptedException e) {
        }
    }
}
