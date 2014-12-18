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

import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.springframework.stereotype.Service;

import br.com.uol.runas.callable.JUnitCaller;
import br.com.uol.runas.factory.ThreadFactoryImpl;
import br.com.uol.runas.loader.JarClassLoader;
import br.com.uol.runas.service.response.JUnitServiceResponse;

@Service
public class JUnitService {

	public JUnitServiceResponse runTests(String path, final String[] suits) throws Exception {

		try(final URLClassLoader loader = new JarClassLoader(path)){
			final ThreadFactory threadFactory = new ThreadFactoryImpl(loader);
			final ExecutorService service = Executors.newSingleThreadExecutor(threadFactory);

			final JUnitServiceResponse jUnitServiceResponse = service.submit(new JUnitCaller(suits)).get();
			service.shutdownNow();

			return jUnitServiceResponse;
		}
	}
}
