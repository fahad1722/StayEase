package com.fahad.stayease.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupConsoleLogger {

	private final Environment environment;

	public StartupConsoleLogger(Environment environment) {
		this.environment = environment;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void logStartupUrls() {
		String port = environment.getProperty("local.server.port",
				environment.getProperty("server.port", "8080"));
		String contextPath = normalizeContextPath(environment.getProperty("server.servlet.context-path", ""));

		String baseUrl = "http://localhost:" + port + contextPath;

		System.out.println("\n============================================================");
		System.out.println("StayEase started successfully");
		System.out.println("Swagger UI: " + baseUrl + "/swagger-ui.html");
		System.out.println("OpenAPI Docs: " + baseUrl + "/v3/api-docs");
		System.out.println("Actuator Health: " + baseUrl + "/actuator/health");
		System.out.println("Actuator Info: " + baseUrl + "/actuator/info");
		System.out.println("============================================================");
	}

	private String normalizeContextPath(String contextPath) {
		if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
			return "";
		}

		String normalized = contextPath.startsWith("/") ? contextPath : "/" + contextPath;
		return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
	}
}
