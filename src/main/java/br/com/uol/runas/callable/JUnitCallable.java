package br.com.uol.runas.callable;

import java.util.concurrent.Callable;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

public class JUnitCallable implements Callable<Result> {
	
	private Class<?>[] classes;

	public JUnitCallable(Class<?>[] classes) {
		this.classes = classes;
	}
	
	@Override
	public Result call() throws Exception {
		return JUnitCore.runClasses(classes);
	}
}
