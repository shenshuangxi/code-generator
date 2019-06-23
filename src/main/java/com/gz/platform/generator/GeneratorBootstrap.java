package com.gz.platform.generator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.tomcat.util.security.MD5Encoder;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;

import com.gz.platform.generator.config.MybatisConfig.DatasourceProperty;

@SpringBootApplication
@MapperScan("com.gz.platform.generator.mapper")
@ComponentScan(basePackages = { "com.gz" })
public class GeneratorBootstrap {
	public static void main(String[] args) {
		System.out.println(DigestUtils.md5DigestAsHex(new String("结算平台-客户数据库").getBytes(StandardCharsets.UTF_8)));
		String md5 =DigestUtils.md5DigestAsHex(new String("结算平台-客户数据库").getBytes(StandardCharsets.UTF_8)).toLowerCase();
		System.out.println(md5);
		
		InputStream ips = null;
		try {
			Map<String, DatasourceProperty> maps = new HashMap<>();
			Properties properties = new Properties();
			ips = GeneratorBootstrap.class.getClassLoader().getResourceAsStream("datasource.properties");
			properties.load(ips);
			for (Entry<Object, Object> entry : properties.entrySet()) {
				System.out.println(entry.getKey()+" : "+entry.getValue());
				String[] mapKeyFields = entry.getKey().toString().split("\\.");
				if (!maps.containsKey(mapKeyFields[0])) {
					maps.put(mapKeyFields[0], new DatasourceProperty());
				}
				DatasourceProperty datasourceProperty = maps.get(mapKeyFields[0]);
				datasourceProperty.setFieldValue(mapKeyFields[1], entry.getValue().toString());
			}
			System.out.println(maps);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (ObjectUtils.isEmpty(ips)) {
				try {
					ips.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		
		SpringApplication.run(GeneratorBootstrap.class, args);
	}
	
	
	
}
