package com.gz.platform.generator.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class DynamicDatasourceFilter extends OncePerRequestFilter {

	@Value("${datasourceHeader:datasourceHeader}")
	private String datasourceHeader;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String datasource = request.getHeader(datasourceHeader);
		DataSourceManager.setDatasource(datasource);
		filterChain.doFilter(request, response);
	}

}
