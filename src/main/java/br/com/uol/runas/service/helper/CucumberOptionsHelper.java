package br.com.uol.runas.service.helper;

import java.lang.annotation.Annotation;

import cucumber.api.CucumberOptions;
import cucumber.api.SnippetType;

public class CucumberOptionsHelper {

	public static Annotation newCucumberOptions(final CucumberOptions oldCucumberOptions, final String[] newFormats) {

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
