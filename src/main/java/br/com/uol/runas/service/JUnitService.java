package br.com.uol.runas.service;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.junit.runner.Result;
import org.springframework.stereotype.Service;

import br.com.uol.runas.callable.JUnitCallable;
import br.com.uol.runas.factory.ThreadFactoryImpl;

@Service
public class JUnitService {
	
	public Result scan(String path, final String[] suits) throws Exception {
		
		URL[] url = new URL[] {new File(path).toURI().toURL()};
		final ClassLoader classLoader = URLClassLoader.newInstance(url);
		
		ThreadFactory threadFactory = new ThreadFactoryImpl(classLoader);			
		
		ExecutorService service = Executors.newSingleThreadExecutor(threadFactory);
		
		Class<?>[] classes = new Class[suits.length];
		
		for (int i = 0; i < suits.length; i++) {
			classes[i] = classLoader.loadClass(suits[i]);
		}
		
		Result result = service.submit(new JUnitCallable(classes)).get();
		
		return result;
	}
}
