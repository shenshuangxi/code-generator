package com.gz.platform.generator.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;

import com.google.common.collect.Maps;
import com.gz.platform.generator.GeneratorBootstrap;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class MybatisConfig {
	
	@Value("${mybatis.mapperLocations:#{classpath:mapper/**/*.xml}}")
	private String mapperLocations;
	
	private Map<Object, Object> datasources = Maps.newConcurrentMap();
	
	private Map<String, DatasourceProperty> datasourceProperties = Maps.newConcurrentMap();
	
	public Map<Object, Object> getDatasources() {
		return datasources;
	}
	
	@Getter
	@Setter
	public static class DatasourceProperty {
		private String name;
		
		private String driver;
		
		private String url;
		
		private String username;
		
		private String password;

		public void setFieldValue(String fieldName, String value) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
			DatasourceProperty.class.getDeclaredField(fieldName).set(this, value);
		}
	}
	

	public void setDatasources(DatasourceProperty datasourceProperty) {
		String md5Name = DigestUtils.md5DigestAsHex(new String(datasourceProperty.getName()).getBytes(StandardCharsets.UTF_8)).toLowerCase();
		Properties properties = new Properties();
		OutputStream ops = null;
		InputStream ips = GeneratorBootstrap.class.getClassLoader().getResourceAsStream("datasource.properties");
		try {
			properties.load(ips);
			for (Field field : datasourceProperty.getClass().getDeclaredFields()) {
				field.setAccessible(true);
				Object value = field.get(datasourceProperty);
				if (!ObjectUtils.isEmpty(value)) {
					properties.put("datasource_"+md5Name+"."+field.getName(), value.toString());
				}
			}
			ops = new FileOutputStream(new File(MybatisConfig.class.getClassLoader().getResource("datasource.properties").toURI()));
			properties.store(ops, "测试");
		} catch (Exception e) {
			log.error(e.getLocalizedMessage(), e);
			throw new RuntimeException(String.format("[%s] 写入配置失败", datasourceProperty.getName()));
		} finally {
			if (!ObjectUtils.isEmpty(ops)) {
				try {
					ops.close();
				} catch (IOException e) {
					log.error(e.getLocalizedMessage(), e);
				}
			}
		}
		datasources.put("datasource_"+md5Name, new PooledDataSource(datasourceProperty.getDriver(), datasourceProperty.getUrl(), datasourceProperty.getUsername(), datasourceProperty.getPassword()));
	}


	{
		InputStream ips = null;
		try {
			Properties properties = new Properties();
			ips = GeneratorBootstrap.class.getClassLoader().getResourceAsStream("datasource.properties");
			properties.load(ips);
			for (Entry<Object, Object> entry : properties.entrySet()) {
				String[] mapKeyFields = entry.getKey().toString().split("\\.");
				if (!datasourceProperties.containsKey(mapKeyFields[0])) {
					datasourceProperties.put(mapKeyFields[0], new DatasourceProperty());
				}
				Field field = DatasourceProperty.class.getDeclaredField(mapKeyFields[1]);
				field.setAccessible(true);
				field.set(datasourceProperties.get(mapKeyFields[0]), entry.getValue().toString());
			}
		} catch (Exception e) {
			log.error(e.getLocalizedMessage(), e);
			System.exit(-1);
		} finally {
			if (ObjectUtils.isEmpty(ips)) {
				try {
					ips.close();
				} catch (IOException e) {
					log.error(e.getLocalizedMessage(), e);
				}
			}
		}
		for (Entry<String, DatasourceProperty> entry : datasourceProperties.entrySet()) {
			String datasourceName = entry.getKey();
			DatasourceProperty datasourceProperty = entry.getValue();
			datasources.put(datasourceName, new PooledDataSource(datasourceProperty.getDriver(), datasourceProperty.getUrl(), datasourceProperty.getUsername(), datasourceProperty.getPassword()));
		}
	}
	
	
	
	@Bean
	public DataSource dataSource() {
		ThreadLocalRountingDataSource threadLocalRountingDataSource =  new ThreadLocalRountingDataSource(datasources);
		return threadLocalRountingDataSource;
	}
	

	@Bean
	public SqlSessionFactory createSqlSessionFactory(DataSource dataSource) throws Exception {
		SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
		sqlSessionFactoryBean.setDataSource(dataSource);
		//添加XML扫描目录
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            List<Resource> listOfResource = new ArrayList<Resource>();
            listOfResource.addAll(Arrays.asList(resolver.getResources(mapperLocations)));
            sqlSessionFactoryBean.setMapperLocations(listOfResource.toArray(new Resource[0]));
        } catch (FileNotFoundException e) {
            log.warn("No Mapper File Found in [" + mapperLocations + "]");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
		SqlSessionFactory sqlSessionFactory = sqlSessionFactoryBean.getObject();
		return sqlSessionFactory;
	}
	

}
