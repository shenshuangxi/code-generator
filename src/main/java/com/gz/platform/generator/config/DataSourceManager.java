package com.gz.platform.generator.config;

import org.springframework.stereotype.Component;

@Component
public class DataSourceManager {

	private static ThreadLocal<String> datasources = new ThreadLocal<>();
	
	public static String getDatasource() {
		return datasources.get();
	}
	
	public static void setDatasource(String datasource) {
		datasources.set(datasource);
	}
	
}
