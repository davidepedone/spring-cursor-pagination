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
package it.davidepedone.scp.data.web;

import it.davidepedone.scp.data.CursorPageable;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.server.mvc.UriComponentsContributor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.springframework.hateoas.TemplateVariable.VariableType.REQUEST_PARAM;
import static org.springframework.hateoas.TemplateVariable.VariableType.REQUEST_PARAM_CONTINUED;

/**
 * Extension of {@link CursorPageableHandlerMethodArgumentResolver} that also supports
 * enhancing URIs using Spring HATEOAS support.
 *
 * @author Davide Pedone
 * @since 1.1
 */
public class HateoasCursorPageableHandlerMethodArgumentResolver extends CursorPageableHandlerMethodArgumentResolver
		implements UriComponentsContributor {

	private static final HateoasSortHandlerMethodArgumentResolver DEFAULT_SORT_RESOLVER = new HateoasSortHandlerMethodArgumentResolver();

	private final HateoasSortHandlerMethodArgumentResolver sortResolver;

	/**
	 * Constructs an instance of this resolver with a default
	 * {@link HateoasSortHandlerMethodArgumentResolver}.
	 */
	public HateoasCursorPageableHandlerMethodArgumentResolver() {
		this(null);
	}

	/**
	 * Creates a new {@link HateoasCursorPageableHandlerMethodArgumentResolver} using the
	 * given {@link HateoasSortHandlerMethodArgumentResolver}..
	 * @param sortResolver
	 */
	public HateoasCursorPageableHandlerMethodArgumentResolver(
			@Nullable HateoasSortHandlerMethodArgumentResolver sortResolver) {

		super(getDefaultedSortResolver(sortResolver));
		this.sortResolver = getDefaultedSortResolver(sortResolver);
	}

	@Override
	public void enhance(UriComponentsBuilder builder, MethodParameter parameter, Object value) {
		Assert.notNull(builder, "UriComponentsBuilder must not be null!");

		if (!(value instanceof CursorPageable)) {
			return;
		}

		CursorPageable pageable = (CursorPageable) value;

		if (pageable.isUnpaged()) {
			return;
		}

		String continuationTokenPropertyName = getParameterNameToUse(getContinuationTokenParameterName(), parameter);
		String sizePropertyName = getParameterNameToUse(getSizeParameterName(), parameter);
		if (StringUtils.hasText(pageable.getContinuationToken())) {
			builder.replaceQueryParam(continuationTokenPropertyName, pageable.getContinuationToken());
		}
		builder.replaceQueryParam(sizePropertyName, Math.min(pageable.getSize(), getMaxPageSize()));

		this.sortResolver.enhance(builder, parameter,
				Optional.ofNullable(pageable.getSort()).map(s -> Sort.by(pageable.getDirection(), s)).orElse(null));
	}

	/**
	 * Returns the template variable for the pagination parameters.
	 * @param parameter can be {@literal null}.
	 * @return
	 */
	public TemplateVariables getPaginationTemplateVariables(MethodParameter parameter, UriComponents template) {

		String continuationTokenPropertyName = getParameterNameToUse(getContinuationTokenParameterName(), parameter);
		String sizePropertyName = getParameterNameToUse(getSizeParameterName(), parameter);

		List<TemplateVariable> names = new ArrayList<>();
		MultiValueMap<String, String> queryParameters = template.getQueryParams();
		boolean append = !queryParameters.isEmpty();

		for (String propertyName : Arrays.asList(continuationTokenPropertyName, sizePropertyName)) {

			if (!queryParameters.containsKey(propertyName)) {

				TemplateVariable.VariableType type = append ? REQUEST_PARAM_CONTINUED : REQUEST_PARAM;
				String description = String.format("pagination.%s.description", propertyName);
				names.add(new TemplateVariable(propertyName, type, description));
			}
		}

		TemplateVariables pagingVariables = new TemplateVariables(names);
		return pagingVariables.concat(sortResolver.getSortTemplateVariables(parameter, template));
	}

	private static HateoasSortHandlerMethodArgumentResolver getDefaultedSortResolver(
			@Nullable HateoasSortHandlerMethodArgumentResolver sortResolver) {
		return sortResolver == null ? DEFAULT_SORT_RESOLVER : sortResolver;
	}

}
