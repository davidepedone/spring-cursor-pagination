/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.davidepedone.scp.config;

import it.davidepedone.scp.data.web.CursorPageableHandlerMethodArgumentResolver;
import it.davidepedone.scp.data.web.config.CursorPageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Optional;

/**
 * JavaConfig class to register {@link CursorPageableHandlerMethodArgumentResolver}
 *
 * @author Davide Pedone
 * @since 1.1
 */
@Configuration
public class SpringCursorPaginationCommonConfiguration implements WebMvcConfigurer {

	@Autowired
	private Optional<CursorPageableHandlerMethodArgumentResolverCustomizer> pageableResolverCustomizer;

	@Autowired
	private Optional<SortHandlerMethodArgumentResolver> sortHandlerMethodArgumentResolver;

	@Bean
	public CursorPageableHandlerMethodArgumentResolver searchFilterHandlerMethodArgumentResolver(
			SortHandlerMethodArgumentResolver sortHandlerMethodArgumentResolver) {
		CursorPageableHandlerMethodArgumentResolver searchFilterHandlerMethodArgumentResolver = new CursorPageableHandlerMethodArgumentResolver(
				sortHandlerMethodArgumentResolver);
		customizePageableResolver(searchFilterHandlerMethodArgumentResolver);
		return searchFilterHandlerMethodArgumentResolver;
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(searchFilterHandlerMethodArgumentResolver(sortHandlerMethodArgumentResolver.orElse(null)));
	}

	protected void customizePageableResolver(CursorPageableHandlerMethodArgumentResolver pageableResolver) {
		pageableResolverCustomizer.ifPresent(c -> c.customize(pageableResolver));
	}

}
