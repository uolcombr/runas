package br.com.uol.runas.factory;

import java.util.concurrent.ThreadFactory;

public class ThreadFactoryImpl implements ThreadFactory {
	
	private ClassLoader classLoader;
	
	public ThreadFactoryImpl(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	
	@Override
	public Thread newThread(Runnable r) {
		
		Thread thread = new Thread(r);
		thread.setContextClassLoader(classLoader);
		
		return thread;
	}

}
