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

import org.junit.runner.Result;
import org.springframework.stereotype.Service;

import br.com.uol.runas.callable.JUnitCallable;
import br.com.uol.runas.factory.ClassLoaderFactory;
import br.com.uol.runas.factory.ThreadFactoryImpl;

@Service
public class JUnitService {

	public Result runTests(String path, final String[] suites) throws Exception {
	    final ClassLoader loader = ClassLoaderFactory.newClassLoader(path);
        final ThreadFactory threadFactory = new ThreadFactoryImpl(loader);
        final ExecutorService service = Executors.newSingleThreadExecutor(threadFactory);

        final Class<?>[] classes = new Class[suites.length];

        for (int i = 0; i < suites.length; i++) {
            classes[i] = loader.loadClass(suites[i]);
        }

        return service.submit(new JUnitCallable(classes)).get();
    }
}
