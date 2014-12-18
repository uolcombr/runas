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

import gherkin.deps.com.google.gson.Gson;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.junit.runner.Result;
import org.junit.runners.Suite;
import org.springframework.stereotype.Service;

import br.com.uol.runas.callable.JUnitCallable;
import br.com.uol.runas.factory.ThreadFactoryImpl;
import br.com.uol.runas.loader.JarClassLoader;
import br.com.uol.runas.service.enums.ContentType;
import br.com.uol.runas.service.response.JUnitServiceResponse;
import cucumber.api.CucumberOptions;
import cucumber.api.SnippetType;

@Service
public class JUnitService {

	private final String LOG_PATH_WITHOUT_EXTENSION = "/home/cad_asilva/test/testLog";
	private Set<Class<?>> classesToChange;
	private Set<ContentType> foundTypes;
	private ContentType contentType;

	private final Map<Class<?>, String> logMap = new HashMap<Class<?>, String>();

	@SuppressWarnings("unchecked")
	public JUnitServiceResponse runTests(String path, final String[] suits) throws Exception {

		try(final URLClassLoader loader = new JarClassLoader(path)){
			final ThreadFactory threadFactory = new ThreadFactoryImpl(loader);
			final ExecutorService service = Executors.newSingleThreadExecutor(threadFactory);
			final Class<?>[] classes = new Class[suits.length];
			classesToChange = new HashSet<>();
			foundTypes = new HashSet<>();
			logMap.clear();
			for (int i = 0; i < suits.length; i++) {
				classes[i] = loader.loadClass(suits[i]);
				prepareClass(classes[i]);
			}

			contentType = chooseContentType();
			setLogPath();
			alterClasses();

			final Result result = service.submit(new JUnitCallable(classes)).get();

			final List<Object> logs = new ArrayList<>();
			for(String log : logMap.values()){
				logs.addAll(new Gson().fromJson(new String(Files.readAllBytes(Paths.get(log))), List.class));
			}

			return new JUnitServiceResponse(contentType.getContentType(), new Gson().toJson(logs) , result);
		}
	}

	private void prepareClass(Class<?> clazz) throws Exception{

		if(clazz.isAnnotationPresent(Suite.SuiteClasses.class)){
			prepareSuiteClasses(clazz);
		}

		if(clazz.isAnnotationPresent(CucumberOptions.class)){
			prepareCucumberClasses(clazz);
		}
	}

	private void prepareSuiteClasses(Class<?> clazz) throws Exception {
		final Suite.SuiteClasses suiteClasses = clazz.getAnnotation(Suite.SuiteClasses.class);

		for(Class<?> c : suiteClasses.value()){

			if(c.isAnnotationPresent(CucumberOptions.class)){
				prepareCucumberClasses(c);
			}
			prepareClass(c);
		}
	}

	private void prepareCucumberClasses(Class<?> clazz) {

		CucumberOptions cucumberOptions = clazz.getAnnotation(CucumberOptions.class);

		if(cucumberOptions.format() != null){
			classesToChange.add(clazz);
			appendFormats(cucumberOptions);
		}
	}

	private void alterClasses() throws Exception{

		Annotation newCucumberOptions;
		for(Class<?> clazz : classesToChange){
			newCucumberOptions = changeCucumberOptions(clazz, clazz.getAnnotation(CucumberOptions.class));
			overrideCucumberOptions(newCucumberOptions, clazz);
		}
	}

	@SuppressWarnings("unchecked")
	private void overrideCucumberOptions(Annotation annotation, Class<?> clazz) throws Exception{
		final Field field = Class.class.getDeclaredField("annotations");
		field.setAccessible(true);
		final Map<Class<? extends Annotation>, Annotation> annotations = (Map<Class<? extends Annotation>, Annotation>) field.get(clazz);
		annotations.put(CucumberOptions.class, annotation);
	}

	private void appendFormats(CucumberOptions cucumberOptions){

		final String[] oldFormats = cucumberOptions.format();
		String[] extensions;

		if(oldFormats != null){

			for(int i = 0; i < oldFormats.length; i++){
				extensions = oldFormats[i].split(":");
				switch (extensions[0]) {

				case "json":
					foundTypes.add(ContentType.JSON);
					break;

				case "xml":
					foundTypes.add(ContentType.XML);
					break;

				case "html":
					foundTypes.add(ContentType.HTML);
					break;

				default:
					break;
				}
			}
		}
	}

	private Annotation changeCucumberOptions(Class<?>clazz, CucumberOptions cucumberOptions){

		return newCucumberOptions(cucumberOptions, new String[]{contentType.getExtension() + ":" + logMap.get(clazz)});
	}

	private ContentType chooseContentType(){

		if(foundTypes.contains(ContentType.JSON)){
			return ContentType.JSON;
		}

		if(foundTypes.contains(ContentType.XML)){
			return ContentType.XML;
		}

		if(foundTypes.contains(ContentType.HTML)){
			return ContentType.HTML;
		}

		return ContentType.JSON;
	}

	private void setLogPath(){

		for(Class<?> clazz : classesToChange){
			logMap.put(clazz, LOG_PATH_WITHOUT_EXTENSION + clazz.getCanonicalName() + "." + contentType.getExtension());
		}
	}

	private Annotation newCucumberOptions(final CucumberOptions oldCucumberOptions, final String[] newFormats) {

		final Annotation newCucumberOptions = new CucumberOptions() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return oldCucumberOptions.annotationType();
			}

			@Override
			public String[] tags() {
				return oldCucumberOptions.tags();
			}

			@Override
			public boolean strict() {
				return oldCucumberOptions.strict();
			}

			@Override
			public SnippetType snippets() {
				return oldCucumberOptions.snippets();
			}

			@Override
			public String[] name() {
				return oldCucumberOptions.name();
			}

			@Override
			public boolean monochrome() {
				return oldCucumberOptions.monochrome();
			}

			@Override
			public String[] glue() {
				return oldCucumberOptions.glue();
			}

			@Override
			public String[] format() {
				return newFormats;
			}

			@Override
			public String[] features() {
				return oldCucumberOptions.features();
			}

			@Override
			public boolean dryRun() {
				return oldCucumberOptions.dryRun();
			}

			@Override
			public String dotcucumber() {
				return oldCucumberOptions.dotcucumber();
			}
		};

		return newCucumberOptions;
	}
}
