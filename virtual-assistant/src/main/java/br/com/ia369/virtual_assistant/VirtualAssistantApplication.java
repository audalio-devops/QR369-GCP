package br.com.ia369.virtual_assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class VirtualAssistantApplication {

	public static void main(String[] args) {
		SpringApplication.run(VirtualAssistantApplication.class, args);
	}

}
