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
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.stereotype.Component;

import br.com.uol.runas.service.helper.Reflections;

@Component
public class ClassLoaderGC implements Runnable {

    private static final Long HUNDRED_MILLIS = 100L;

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
        while (isNotThreadInterrupted()) {
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
        releaseFromJdbc(classLoader);
        releaseFromLoggers(classLoader);
        releaseFromBeans(classLoader);
        releaseFromShutdownHooks(classLoader);
        releaseFromThreads(classLoader);
    }

    private void releaseFromJdbc(WeakReference<ClassLoader> classLoader) {
        try {
            deregisterJdbcDrivers(classLoader);
            deregisterOracleDiagnosabilityMBean(classLoader);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void deregisterJdbcDrivers(WeakReference<ClassLoader> classLoader) {
        final List<?> drivers = Reflections.getStaticFieldValue(DriverManager.class, "registeredDrivers");

        final Map<Object, Driver> toDeregister = new HashMap<>();

        for (Object info : drivers) {
            final Driver driver = Reflections.getFieldValue(info, "driver");

            if (Objects.equals(classLoader.get(), driver.getClass().getClassLoader())) {
                toDeregister.put(info, driver);
            }
        }

        drivers.removeAll(toDeregister.keySet());
    }

    private void deregisterOracleDiagnosabilityMBean(WeakReference<ClassLoader> classLoader) {
        try {
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            final Hashtable<String, String> keys = new Hashtable<String, String>();
            keys.put("type", "diagnosability");
            keys.put("name", classLoader.get().getClass().getName() + "@" + Integer.toHexString(classLoader.get().hashCode()).toLowerCase());
            mbs.unregisterMBean(new ObjectName("com.oracle.jdbc", keys));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void releaseFromShutdownHooks(WeakReference<ClassLoader> classLoader) {
        final Map<Thread, Thread> hooks = (Map<Thread, Thread>) Reflections.getStaticFieldValue("java.lang.ApplicationShutdownHooks", "hooks");

        if(hooks != null) {
            final List<Thread> shutdownHooks = new ArrayList<>(hooks.keySet());

            for(Thread shutdownHook : shutdownHooks) {
                if (Objects.equals(classLoader.get(), shutdownHook.getContextClassLoader())) {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                    shutdownHook.start();
                    try {
                        shutdownHook.join(500);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } finally {
                        shutdownHook.stop();
                    }
                    shutdownHook.setContextClassLoader(null);
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
        try {
            final Class<?> logFactoryClass = classLoader.get().loadClass("org.apache.commons.logging.LogFactory");
            final Method releaseMethod = logFactoryClass.getDeclaredMethod("release", ClassLoader.class);
            releaseMethod.invoke(null, classLoader.get());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            final Class<?> logginHandlerClass = classLoader.get().loadClass("org.openqa.selenium.logging.LoggingHandler");
            final Object instance = Reflections.getStaticFieldValue(logginHandlerClass, "instance");
            final Method closeMethod = logginHandlerClass.getDeclaredMethod("close");

            closeMethod.invoke(instance, (Object[]) null);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            final Class<?> remoteWebDriverClass = classLoader.get().loadClass("org.openqa.selenium.remote.RemoteWebDriver");
            final Object logger = Reflections.getStaticFieldValue(remoteWebDriverClass, "logger");
           // final Class<?> loggingClass = classLoader.get().loadClass("java.util.logging.Logger");
            final List<?> handlers = Reflections.getFieldValue(logger, "handlers");
            handlers.clear();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void forceGC(WeakReference<?> reference) {
        final int maxAttempts = 20;

        for (int attempt = 1; attempt <= maxAttempts && isNotThreadInterrupted(); attempt++) {
            System.gc();

            if (reference.get() == null) {
                System.err.println("GCed!");
                break;
            } else if (attempt == maxAttempts) {
                System.err.println("Potential ClassLoader Leak --->" + reference.get());
            } else {
                sleep();
            }
        }
    }

    private void sleep() {
        try {
            Thread.sleep(HUNDRED_MILLIS);
        } catch (InterruptedException e) {
        }
    }

    private boolean isNotThreadInterrupted() {
        return !Thread.currentThread().isInterrupted();
    }
}
