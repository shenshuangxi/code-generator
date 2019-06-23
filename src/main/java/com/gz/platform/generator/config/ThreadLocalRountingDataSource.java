package com.gz.platform.generator.config;

import java.util.Map;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;

public class ThreadLocalRountingDataSource extends AbstractRoutingDataSource {

	
	public ThreadLocalRountingDataSource(DataSourceLookup dataSourceLookup) {
		this.setDataSourceLookup(dataSourceLookup);
	}
	
	public ThreadLocalRountingDataSource(Map<Object, Object> targetDataSources) {
		this.setTargetDataSources(targetDataSources);
	}

	@Override
	protected Object determineCurrentLookupKey() {
		return DataSourceManager.getDatasource();
	}

}
