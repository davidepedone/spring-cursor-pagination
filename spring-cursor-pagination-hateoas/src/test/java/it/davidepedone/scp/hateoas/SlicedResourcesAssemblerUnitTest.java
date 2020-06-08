/*
 * Copyright 2013-2020 the original author or authors.
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
package it.davidepedone.scp.hateoas;

import it.davidepedone.scp.pagination.CursorPaginationSlice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit tests for {@link SlicedResourcesAssembler}.
 *
 * @author Davide Pedone
 */
class SlicedResourcesAssemblerUnitTest {

	HateoasPageableHandlerMethodArgumentResolver resolver = new HateoasPageableHandlerMethodArgumentResolver();

	SlicedResourcesAssembler<Person> assembler = new SlicedResourcesAssembler<>(resolver, null);

	@BeforeEach
	void setUp() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);
	}

	@Test
	void addsNextLinkForFirstPage() {
		SlicedModel<EntityModel<Person>> resources = assembler.toModel(createSlice(5, "notarealtoken"));
		assertThat(resources.getLink(IanaLinkRelations.PREV)).isEmpty();
		assertThat(resources.getLink(IanaLinkRelations.FIRST)).isNotEmpty();
		assertThat(resources.getLink(IanaLinkRelations.SELF)).isNotEmpty();
		assertThat(resources.getLink(IanaLinkRelations.NEXT)).isNotEmpty();
	}

	@Test
	void doNotAddNextLinkForLastPage() {
		SlicedModel<EntityModel<Person>> resources = assembler.toModel(createSlice(5, null));
		assertThat(resources.getLink(IanaLinkRelations.PREV)).isEmpty();
		assertThat(resources.getLink(IanaLinkRelations.FIRST)).isNotEmpty();
		assertThat(resources.getLink(IanaLinkRelations.SELF)).isNotEmpty();
		assertThat(resources.getLink(IanaLinkRelations.NEXT)).isEmpty();
	}

	@Test
	void usesBaseUriIfConfigured() {

		UriComponents baseUri = UriComponentsBuilder.fromUriString("https://foo:9090").build();

		SlicedResourcesAssembler<Person> assembler = new SlicedResourcesAssembler<>(resolver, baseUri);
		SlicedModel<EntityModel<Person>> resources = assembler.toModel(createSlice(1, "token"));

		assertThat(resources.getRequiredLink(IanaLinkRelations.SELF).getHref()).startsWith(baseUri.toUriString());
		assertThat(resources.getRequiredLink(IanaLinkRelations.NEXT).getHref()).startsWith(baseUri.toUriString());
	}

	@Test
	void usesCustomLinkProvided() {

		Link link = Link.of("https://foo:8080", "rel");

		SlicedModel<EntityModel<Person>> resources = assembler.toModel(createSlice(1, "token"), link);

		assertThat(resources.getRequiredLink(IanaLinkRelations.SELF)).isEqualTo(link.withSelfRel());
		assertThat(resources.getRequiredLink(IanaLinkRelations.NEXT).getHref()).startsWith(link.getHref());
	}

	@Test
	@DisplayName("First page link should not contain continuationToken")
	void firstPageLink() {
		SlicedModel<EntityModel<Person>> resources = assembler.toModel(createSlice(5, "notarealtoken"));
		assertFalse(resources.getRequiredLink(IanaLinkRelations.FIRST).getHref().contains("continuationToken="));
	}

	@Test
	@DisplayName("Next page link should contain continuationToken")
	void nextPageLink() {
		SlicedModel<EntityModel<Person>> resources = assembler.toModel(createSlice(5, "notarealtoken"));
		assertThat(resources.getRequiredLink(IanaLinkRelations.NEXT).getHref()
				.contains("continuationToken=notarealtoken"));
	}

	private static CursorPaginationSlice<Person> createSlice(int size, String token) {
		Person person = new Person();
		person.name = "Davide";
		return new CursorPaginationSlice<>(Collections.singletonList(person), size, token);
	}

	static class Person {

		String name;

	}

}