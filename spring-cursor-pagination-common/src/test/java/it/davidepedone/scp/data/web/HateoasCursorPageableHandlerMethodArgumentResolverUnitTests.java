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

import it.davidepedone.scp.data.CursorPageRequest;
import it.davidepedone.scp.data.CursorPageable;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.hateoas.server.mvc.UriComponentsContributor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link HateoasCursorPageableHandlerMethodArgumentResolver}
 *
 * @author Davide Pedone
 */
class HateoasCursorPageableHandlerMethodArgumentResolverUnitTests
		extends CursorPageableHandlerMethodArgumentResolverUnitTests {

	@Test
	void buildsUpRequestParameters() {

		String basicString = String.format("size=%d", PAGE_SIZE);

		assertUriStringFor(REFERENCE_WITHOUT_SORT, basicString);
		assertUriStringFor(REFERENCE_WITH_SORT_DESC, basicString + "&sort=firstname,desc");
		assertUriStringFor(REFERENCE_WITH_SORT_ASC, basicString + "&sort=firstname,asc");
	}

	@Test
	void replacesExistingPaginationInformation() throws Exception {

		MethodParameter parameter = new MethodParameter(Sample.class.getMethod("supportedMethod", CursorPageable.class),
				0);
		UriComponentsContributor resolver = new HateoasCursorPageableHandlerMethodArgumentResolver();
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://localhost:8080?page=0&size=10");
		resolver.enhance(builder, parameter, CursorPageRequest.of("token", 20));

		MultiValueMap<String, String> params = builder.build().getQueryParams();

		List<String> continuationToken = params.get("continuationToken");
		assertThat(continuationToken).hasSize(1);
		assertThat(continuationToken.get(0)).isEqualTo("token");

		List<String> size = params.get("size");
		assertThat(size).hasSize(1);
		assertThat(size.get(0)).isEqualTo("20");
	}

	@Test
	void preventsPageSizeFromExceedingMayValueIfConfiguredOnWrite() throws Exception {
		assertUriStringFor(CursorPageRequest.of("token", 200), "continuationToken=token&size=100");
	}

	@Test
	void appendsTemplateVariablesCorrectly() {
		assertTemplateEnrichment("/foo", "{?continuationToken,size,sort}");
		assertTemplateEnrichment("/foo?bar=1", "{&continuationToken,size,sort}");
		assertTemplateEnrichment("/foo?continuationToken=1", "{&size,sort}");
		assertTemplateEnrichment("/foo?continuationToken=1&size=10", "{&sort}");
		assertTemplateEnrichment("/foo?continuationToken=1&sort=foo,asc", "{&size}");
		assertTemplateEnrichment("/foo?continuationToken=1&size=10&sort=foo,asc", "");
	}

	@Test
	void returnsCustomizedTemplateVariables() {

		UriComponents uriComponents = UriComponentsBuilder.fromPath("/foo").build();

		HateoasCursorPageableHandlerMethodArgumentResolver resolver = getResolver();
		resolver.setContinuationTokenParameterName("foo");
		String variables = resolver.getPaginationTemplateVariables(null, uriComponents).toString();

		assertThat(variables).isEqualTo("{?foo,size,sort}");
	}

	@Test
	void enhancesUnpaged() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/");
		getResolver().enhance(builder, null, CursorPageable.unpaged());
		assertThat(builder.toUriString()).isEqualTo("/?size=20");
	}

	@Override
	protected HateoasCursorPageableHandlerMethodArgumentResolver getResolver() {

		HateoasCursorPageableHandlerMethodArgumentResolver resolver = new HateoasCursorPageableHandlerMethodArgumentResolver();
		resolver.setMaxPageSize(100);
		return resolver;
	}

	protected void assertUriStringFor(CursorPageable pageable, String expected) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/");
		MethodParameter parameter = getParameterOfMethod("supportedMethod");

		getResolver().enhance(builder, parameter, pageable);

		assertThat(builder.build().toUriString()).endsWith(expected);
	}

	private void assertTemplateEnrichment(String baseUri, String expected) {

		UriComponents uriComponents = UriComponentsBuilder.fromUriString(baseUri).build();

		HateoasCursorPageableHandlerMethodArgumentResolver resolver = getResolver();
		assertThat(resolver.getPaginationTemplateVariables(null, uriComponents).toString()).isEqualTo(expected);
	}

}