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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadFactoryImpl implements ThreadFactory {

    private final AtomicLong threadId;
    private final WeakReference<ClassLoader> classLoader;

    public ThreadFactoryImpl(ClassLoader classLoader) {
        this.threadId = new AtomicLong(0);
        this.classLoader = new WeakReference<ClassLoader>(classLoader);
    }

    @Override
    public Thread newThread(Runnable r) {
        final Thread thread = new Thread(r, "RunAsThread-" + threadId.incrementAndGet()) {
            @Override
            protected void finalize() throws Throwable {
                setContextClassLoader(null);
                super.finalize();
            }
        };

        if (classLoader != null) {
            thread.setContextClassLoader(classLoader.get());
        }

        return thread;
    }
}
