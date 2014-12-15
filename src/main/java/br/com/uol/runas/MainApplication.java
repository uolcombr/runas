package br.com.uol.runas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@EnableAutoConfiguration
@ComponentScan
public class MainApplication {
	
	public static void main(String[] args) {
		System.getProperties().put("server.port", 8195);
		SpringApplication.run(MainApplication.class, args);
	}

}
