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
import it.davidepedone.scp.data.web.WebTestUtils;
import it.davidepedone.scp.data.web.config.CursorPageableHandlerMethodArgumentResolverCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link SpringCursorPaginationCommonConfiguration}
 *
 * @author Davide Pedone
 */
class SpringCursorPaginationCommonConfigurationIntegrationTests {

	@SpringBootApplication
	static class SampleConfig {

	}

	@SpringBootApplication
	static class CursorPageableResolverCustomizerConfig extends SampleConfig {

		@Bean
		CursorPageableHandlerMethodArgumentResolverCustomizer testCursorPageableResolverCustomizer() {
			return r -> {
				r.setContinuationTokenParameterName("ct");
				r.setSizeParameterName("sz");
				r.setMaxPageSize(10);
				r.setPrefix("A");
				r.setQualifierDelimiter("B");
			};
		}

	}

	@Test
	void registersBasicBeanDefinitions() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("cursorPageableResolver", "sortResolver");

		assertResolversRegistered(context, SortHandlerMethodArgumentResolver.class,
				CursorPageableHandlerMethodArgumentResolver.class);
	}

	@Test
	void picksUpCursorPageableResolverCustomizer() {

		ApplicationContext context = WebTestUtils
				.createApplicationContext(CursorPageableResolverCustomizerConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());
		CursorPageableHandlerMethodArgumentResolver resolver = context.getBean("cursorPageableResolver",
				CursorPageableHandlerMethodArgumentResolver.class);

		assertThat(names).contains("testCursorPageableResolverCustomizer");
		assertThat((String) ReflectionTestUtils.getField(resolver, "continuationTokenParameterName")).isEqualTo("ct");
		assertThat((String) ReflectionTestUtils.getField(resolver, "sizeParameterName")).isEqualTo("sz");
		assertThat((Integer) ReflectionTestUtils.getField(resolver, "maxPageSize")).isEqualTo(10);
		assertThat((String) ReflectionTestUtils.getField(resolver, "prefix")).isEqualTo("A");
		assertThat((String) ReflectionTestUtils.getField(resolver, "qualifierDelimiter")).isEqualTo("B");
	}

	private static void assertResolversRegistered(ApplicationContext context, Class<?>... resolverTypes) {

		RequestMappingHandlerAdapter adapter = context.getBean(RequestMappingHandlerAdapter.class);
		assertThat(adapter).isNotNull();
		List<HandlerMethodArgumentResolver> resolvers = adapter.getCustomArgumentResolvers();

		Arrays.asList(resolverTypes).forEach(type -> assertThat(resolvers).hasAtLeastOneElementOfType(type));
	}

}