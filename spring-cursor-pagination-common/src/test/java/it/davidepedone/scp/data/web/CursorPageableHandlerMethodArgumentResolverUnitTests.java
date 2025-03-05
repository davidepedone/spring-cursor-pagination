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
import it.davidepedone.scp.data.CursorPageableDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static it.davidepedone.scp.data.web.CursorPageableHandlerMethodArgumentResolverSupport.DEFAULT_PAGE_REQUEST;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit test for {@link CursorPageableHandlerMethodArgumentResolver}
 *
 * @author Davide Pedone
 */
class CursorPageableHandlerMethodArgumentResolverUnitTests extends CursorPageableDefaultUnitTests {

	MethodParameter supportedMethodParameter;

	@BeforeEach
	void setUp() throws Exception {
		this.supportedMethodParameter = new MethodParameter(
				Sample.class.getMethod("supportedMethod", CursorPageable.class), 0);
	}

	@Test
	void preventsPageSizeFromExceedingMayValueIfConfigured() throws Exception {
		// Read side
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("size", "200");
		assertSupportedAndResult(supportedMethodParameter, CursorPageRequest.of(100), request);
	}

	@Test
	void rejectsEmptyContinuationTokenParameterName() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new CursorPageableHandlerMethodArgumentResolver().setContinuationTokenParameterName(""));
	}

	@Test
	void rejectsNullContinuationTokenParameterName() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new CursorPageableHandlerMethodArgumentResolver().setContinuationTokenParameterName(null));
	}

	@Test
	void rejectsEmptySizeParameterName() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new CursorPageableHandlerMethodArgumentResolver().setSizeParameterName(""));
	}

	@Test
	void rejectsNullSizeParameterName() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new CursorPageableHandlerMethodArgumentResolver().setSizeParameterName(null));
	}

	@Test
	void qualifierIsUsedInParameterLookup() throws Exception {

		MethodParameter parameter = new MethodParameter(Sample.class.getMethod("validQualifier", CursorPageable.class),
				0);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo_size", "10");
		request.addParameter("foo_continuationToken", "whatatoken");

		assertSupportedAndResult(parameter, CursorPageRequest.of("whatatoken", 10), request);
	}

	@Test
	void usesDefaultPageSizeIfRequestPageSizeIsLessThanOne() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("size", "0");

		assertSupportedAndResult(supportedMethodParameter, DEFAULT_PAGE_REQUEST, request);
	}

	@Test
	void rejectsInvalidCustomDefaultForPageSize() throws Exception {

		MethodParameter parameter = new MethodParameter(
				Sample.class.getMethod("invalidDefaultPageSize", CursorPageable.class), 0);

		assertThatIllegalStateException().isThrownBy(() -> assertSupportedAndResult(parameter, DEFAULT_PAGE_REQUEST)) //
			.withMessageContaining("invalidDefaultPageSize");
	}

	@Test
	void fallsBackToFirstPageIfNegativePageNumberIsGiven() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("size", "-1");

		assertSupportedAndResult(supportedMethodParameter, DEFAULT_PAGE_REQUEST, request);
	}

	@Test
	void pageParamIsNotNumeric() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("size", "a");

		assertSupportedAndResult(supportedMethodParameter, DEFAULT_PAGE_REQUEST, request);
	}

	@Test
	void sizeParamIsNotNumeric() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("size", "a");

		assertSupportedAndResult(supportedMethodParameter, DEFAULT_PAGE_REQUEST, request);
	}

	@Test
	void returnsDefaultIfFallbackIsUnpagedAndNoParametersGiven() throws Exception {

		CursorPageableHandlerMethodArgumentResolver resolver = getResolver();
		resolver.setFallbackPageable(CursorPageable.unpaged());

		assertSupportedAndResult(supportedMethodParameter, CursorPageRequest.of(null, 20, null, Sort.Direction.DESC),
				new ServletWebRequest(new MockHttpServletRequest()), resolver);
	}

	@Test
	void returnsDefaultIfFallbackIsUnpagedAndOnlyContinuationTokenIsGiven() throws Exception {

		CursorPageableHandlerMethodArgumentResolver resolver = getResolver();
		resolver.setFallbackPageable(CursorPageable.unpaged());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("continuationToken", "notarealtoken");

		assertThat(resolver.resolveArgument(supportedMethodParameter, null, new ServletWebRequest(request), null))
			.isEqualTo(CursorPageRequest.of("notarealtoken", 20, null, Sort.Direction.DESC));
	}

	@Test
	void usesNullSortIfNoDefaultIsConfiguredAndPageAndSizeAreGiven() {

		CursorPageableHandlerMethodArgumentResolver resolver = getResolver();
		resolver.setFallbackPageable(CursorPageable.unpaged());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("size", "10");

		CursorPageable result = resolver.resolveArgument(supportedMethodParameter, null, new ServletWebRequest(request),
				null);

		assertThat(result.getSize()).isEqualTo(10);
		assertThat(result.getSort()).isNull();
	}

	@Test
	void detectsFallbackPageableIfNullOneIsConfigured() {

		CursorPageableHandlerMethodArgumentResolver resolver = getResolver();
		resolver.setFallbackPageable(CursorPageable.unpaged());

		assertThat(resolver.isFallbackPageable(null)).isFalse();
		assertThat(resolver.isFallbackPageable(CursorPageRequest.of(10))).isFalse();
	}

	@Override
	protected CursorPageableHandlerMethodArgumentResolver getResolver() {
		CursorPageableHandlerMethodArgumentResolver resolver = new CursorPageableHandlerMethodArgumentResolver();
		resolver.setMaxPageSize(100);
		return resolver;
	}

	@Override
	protected Class<?> getControllerClass() {
		return Sample.class;
	}

	interface Sample {

		void supportedMethod(CursorPageable pageable);

		void unsupportedMethod(String string);

		void invalidDefaultPageSize(@CursorPageableDefault(size = 0) CursorPageable pageable);

		void simpleDefault(@CursorPageableDefault(size = PAGE_SIZE) CursorPageable pageable);

		void simpleDefaultWithSort(
				@CursorPageableDefault(size = PAGE_SIZE, sort = "firstname") CursorPageable pageable);

		void simpleDefaultWithSortAndDirection(@CursorPageableDefault(size = PAGE_SIZE, sort = "firstname",
				direction = Sort.Direction.ASC) CursorPageable pageable);

		void simpleDefaultWithExternalSort(@CursorPageableDefault(size = PAGE_SIZE) @SortDefault(sort = "firstname",
				direction = Sort.Direction.ASC) CursorPageable pageable);

		void simpleDefaultWithContaineredExternalSort(@CursorPageableDefault(size = PAGE_SIZE) //
		@SortDefault.SortDefaults(@SortDefault(sort = "firstname",
				direction = Sort.Direction.ASC)) CursorPageable pageable);

		void invalidQualifiers(@Qualifier("foo") CursorPageable first, @Qualifier("foo") CursorPageable second);

		void validQualifier(@Qualifier("foo") CursorPageable pageable);

		void noQualifiers(CursorPageable first, CursorPageable second);

	}

}
