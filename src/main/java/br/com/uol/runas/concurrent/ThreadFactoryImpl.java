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
package br.com.uol.runas.concurrent;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadFactoryImpl implements ThreadFactory {

    private static final AtomicLong THREAD_ID = new AtomicLong(0);
    private static final AtomicLong GROUP_ID = new AtomicLong(0);
    private final ThreadGroup threadGroup = new ThreadGroup("RunAs-Group#" + GROUP_ID.incrementAndGet());
    private final WeakReference<ClassLoader> classLoader;

    public ThreadFactoryImpl(ClassLoader classLoader) {
        this.classLoader = new WeakReference<ClassLoader>(classLoader);
    }

    @Override
    public Thread newThread(Runnable runnable) {
        return new ThreadOfRunAs(runnable, classLoader.get());
    }

    class ThreadOfRunAs extends Thread {
        public ThreadOfRunAs(Runnable runnable, ClassLoader classLoader) {
            super(threadGroup, runnable, "RunAs-Thread#" + THREAD_ID.incrementAndGet());
            setContextClassLoader(classLoader);
        }

        @Override
        protected void finalize() throws Throwable {
            stopChildrenThreads();
            setContextClassLoader(null);
            super.finalize();
        }

        @SuppressWarnings("deprecation")
        private void stopChildrenThreads() {
            try {
                threadGroup.interrupt();
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (Thread thread : getAllStackTraces().keySet()) {
                if (Objects.equals(threadGroup, thread.getThreadGroup()) && thread.isAlive()) {

                    System.err.println(thread + " killed!");

                    try {
                        thread.interrupt();
                        thread.join(100);
                        thread.stop();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } finally {
                        thread.setContextClassLoader(null);
                    }
                }
            }
        }
    }
}
