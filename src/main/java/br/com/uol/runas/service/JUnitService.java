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
package br.com.uol.runas.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.uol.runas.classloader.ClassLoaderGC;
import br.com.uol.runas.classloader.JarClassLoader;
import br.com.uol.runas.concurrent.JUnitTask;
import br.com.uol.runas.concurrent.ThreadFactoryImpl;
import br.com.uol.runas.service.response.JUnitServiceResponse;

@Service
public class JUnitService {

    @Autowired
    private ClassLoaderGC classLoaderGC;

    public JUnitServiceResponse runTests(String path, final String[] suites) throws Exception {
        try (final JarClassLoader classLoader = new JarClassLoader(path, classLoaderGC)) {
            final ThreadFactory threadFactory = new ThreadFactoryImpl(classLoader);

            final ExecutorService service = Executors.newSingleThreadExecutor(threadFactory);

            try {
                return service.submit(new JUnitTask(suites)).get();
            } finally {
                service.shutdownNow();
            }
        }
    }
}
