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
import it.davidepedone.scp.hateoas.SlicedResourcesAssembler;
import it.davidepedone.scp.data.web.HateoasCursorPageableHandlerMethodArgumentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.lang.Nullable;

import java.util.Optional;

/**
 * JavaConfig class to register {@link SlicedResourcesAssembler} and
 * {@link HateoasCursorPageableHandlerMethodArgumentResolver}
 *
 * @author Davide Pedone
 * @since 1.0
 */
@Configuration
public class SpringCursorPaginationHateoasConfiguration {

	@Autowired
	private Optional<CursorPageableHandlerMethodArgumentResolverCustomizer> pageableResolverCustomizer;

	private HateoasCursorPageableHandlerMethodArgumentResolver hateoasSearchFilterHandlerMethodArgumentResolver(
			HateoasSortHandlerMethodArgumentResolver hateoasSortHandlerMethodArgumentResolver) {
		HateoasCursorPageableHandlerMethodArgumentResolver resolver = new HateoasCursorPageableHandlerMethodArgumentResolver(
				hateoasSortHandlerMethodArgumentResolver);
		customizePageableResolver(resolver);
		return resolver;
	}

	@Bean
	public SlicedResourcesAssembler slicedResourcesAssembler(
			@Nullable HateoasSortHandlerMethodArgumentResolver hateoasSortHandlerMethodArgumentResolver) {
		return new SlicedResourcesAssembler<>(
				hateoasSearchFilterHandlerMethodArgumentResolver(hateoasSortHandlerMethodArgumentResolver), null);
	}

	protected void customizePageableResolver(CursorPageableHandlerMethodArgumentResolver pageableResolver) {
		pageableResolverCustomizer.ifPresent(c -> c.customize(pageableResolver));
	}

}
