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
package it.davidepedone.scp.hateoas;

import it.davidepedone.scp.pagination.CursorPaginationSlice;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * {@link RepresentationModelAssembler} to easily convert {@link CursorPaginationSlice}
 * instances into {@link SlicedModel}.
 *
 * @author Davide Pedone
 * @since 1.0
 */
public class SlicedResourcesAssembler<T>
		implements RepresentationModelAssembler<CursorPaginationSlice<T>, SlicedModel<EntityModel<T>>> {

	private final HateoasPageableHandlerMethodArgumentResolver pageableResolver;

	private final Optional<UriComponents> baseUri;

	private final EmbeddedWrappers wrappers = new EmbeddedWrappers(false);

	private static final String CONTINUATION_TOKEN_QUERY_PARAM = "continuationToken";

	/**
	 * Creates a new {@link SlicedResourcesAssembler} using the given
	 * {@link PageableHandlerMethodArgumentResolver} and base URI. If the former is
	 * {@literal null}, a default one will be created. If the latter is {@literal null},
	 * calls to {@link #toModel(CursorPaginationSlice)} will use the current request's URI
	 * to build the relevant previous and next links.
	 * @param resolver hateoas resolver
	 * @param baseUri can be {@literal null}.
	 * @return SlicedResourcesAssembler
	 */
	public SlicedResourcesAssembler(@Nullable HateoasPageableHandlerMethodArgumentResolver resolver,
			@Nullable UriComponents baseUri) {
		this.pageableResolver = null == resolver ? new HateoasPageableHandlerMethodArgumentResolver() : resolver;
		this.baseUri = Optional.ofNullable(baseUri);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.hateoas.server.RepresentationModelAssembler#toModel(java.lang.
	 * Object)
	 */
	@Override
	@SuppressWarnings("null")
	public SlicedModel<EntityModel<T>> toModel(CursorPaginationSlice<T> slice) {
		return toModel(slice, EntityModel::of);
	}

	/**
	 * Creates a new {@link SlicedModel} by converting the given {@link Slice} into a
	 * {@link SlicedModel.SliceMetadata} instance and wrapping the contained elements into
	 * {@link EntityModel} instances. Will add pagination links based on the given the
	 * self link.
	 * @param slice must not be {@literal null}.
	 * @param selfLink must not be {@literal null}.
	 * @return SlicedModel
	 */
	public SlicedModel<EntityModel<T>> toModel(CursorPaginationSlice<T> slice, Link selfLink) {
		return toModel(slice, EntityModel::of, selfLink);
	}

	/**
	 * Creates a new {@link SlicedModel} by converting the given {@link Slice} into a
	 * {@link SlicedModel.SliceMetadata} instance and using the given
	 * {@link RepresentationModelAssembler} to turn elements of the {@link Slice} into
	 * resources.
	 * @param slice must not be {@literal null}.
	 * @param assembler must not be {@literal null}.
	 * @return SlicedModel
	 */
	public <R extends RepresentationModel<?>> SlicedModel<R> toModel(CursorPaginationSlice<T> slice,
			RepresentationModelAssembler<T, R> assembler) {
		return createModel(slice, assembler, Optional.empty());
	}

	/**
	 * Creates a new {@link SlicedModel} by converting the given {@link Slice} into a
	 * {@link SlicedModel.SliceMetadata} instance and using the given
	 * {@link RepresentationModelAssembler} to turn elements of the {@link Slice} into
	 * resources. Will add pagination links based on the given the self link.
	 * @param slice must not be {@literal null}.
	 * @param assembler must not be {@literal null}.
	 * @param link must not be {@literal null}.
	 * @return SlicedModel
	 */
	public <R extends RepresentationModel<?>> SlicedModel<R> toModel(CursorPaginationSlice<T> slice,
			RepresentationModelAssembler<T, R> assembler, Link link) {

		Assert.notNull(link, "Link must not be null!");

		return createModel(slice, assembler, Optional.of(link));
	}

	/**
	 * Creates a {@link SlicedModel} with an empty collection {@link EmbeddedWrapper} for
	 * the given domain type.
	 * @param page must not be {@literal null}, content must be empty.
	 * @param type must not be {@literal null}.
	 * @return SlicedModel
	 * @since 1.0
	 */
	public SlicedModel<?> toEmptyModel(CursorPaginationSlice<?> page, Class<?> type) {
		return toEmptyModel(page, type, Optional.empty());
	}

	/**
	 * Creates a {@link SlicedModel} with an empt collection {@link EmbeddedWrapper} for
	 * the given domain type.
	 * @param page must not be {@literal null}, content must be empty.
	 * @param type must not be {@literal null}.
	 * @param link must not be {@literal null}.
	 * @return SlicedModel
	 * @since 1.0
	 */
	public SlicedModel<?> toEmptyModel(CursorPaginationSlice<?> page, Class<?> type, Link link) {
		return toEmptyModel(page, type, Optional.of(link));
	}

	private SlicedModel<?> toEmptyModel(CursorPaginationSlice<?> page, Class<?> type, Optional<Link> link) {

		Assert.notNull(page, "Page must not be null!");
		Assert.isTrue(!page.hasContent(), "Page must not have any content!");
		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(link, "Link must not be null!");

		SlicedModel.SliceMetadata metadata = asSliceMetadata(page);

		EmbeddedWrapper wrapper = wrappers.emptyCollectionOf(type);
		List<EmbeddedWrapper> embedded = Collections.singletonList(wrapper);

		return addPaginationLinks(SlicedModel.of(embedded, metadata), page, link);
	}

	/**
	 * Creates the {@link SlicedModel} to be equipped with pagination links downstream.
	 * @param resources the original page's elements mapped into
	 * {@link RepresentationModel} instances.
	 * @param metadata the calculated {@link SlicedModel.SliceMetadata}, must not be
	 * {@literal null}.
	 * @param page the original page handed to the assembler, must not be {@literal null}.
	 * @return must not be {@literal null}.
	 */
	protected <R extends RepresentationModel<?>, S> SlicedModel<R> createPagedModel(List<R> resources,
			SlicedModel.SliceMetadata metadata, CursorPaginationSlice<S> page) {

		Assert.notNull(resources, "Content resources must not be null!");
		Assert.notNull(metadata, "SliceMetadata must not be null!");
		Assert.notNull(page, "Page must not be null!");

		return SlicedModel.of(resources, metadata);
	}

	private <S, R extends RepresentationModel<?>> SlicedModel<R> createModel(CursorPaginationSlice<S> slice,
			RepresentationModelAssembler<S, R> assembler, Optional<Link> link) {

		Assert.notNull(slice, "Slice must not be null!");
		Assert.notNull(assembler, "ResourceAssembler must not be null!");

		List<R> resources = new ArrayList<>(slice.getNumberOfElements());

		for (S element : slice.getContent()) {
			resources.add(assembler.toModel(element));
		}

		SlicedModel<R> resource = createPagedModel(resources, asSliceMetadata(slice), slice);

		return addPaginationLinks(resource, slice, link);
	}

	private <R> SlicedModel<R> addPaginationLinks(SlicedModel<R> resources, CursorPaginationSlice<?> slice,
			Optional<Link> link) {

		UriTemplate base = getUriTemplate(link);

		UriComponentsBuilder builder = UriComponentsBuilder.fromUri(base.expand());
		builder.replaceQueryParam(CONTINUATION_TOKEN_QUERY_PARAM);

		resources.add(
				createLink(UriTemplate.of(builder.build().toString()), null, null, IanaLinkRelations.FIRST.value()));

		Link selfLink = link.map(Link::withSelfRel)//
				.orElseGet(() -> createLink(base, null, IanaLinkRelations.SELF.value()));

		resources.add(selfLink);

		if (slice.hasNext()) {
			resources.add(createLink(base, null, slice.getContinuationToken(), IanaLinkRelations.NEXT.value()));
		}

		return resources;
	}

	/**
	 * Returns a default URI string either from the one configured on assembler creation
	 * or by looking it up from the current request.
	 * @return UriTemplate
	 */
	private UriTemplate getUriTemplate(Optional<Link> baseLink) {
		return UriTemplate.of(baseLink.map(Link::getHref).orElseGet(this::baseUriOrCurrentRequest));
	}

	/**
	 * Creates a {@link Link} with the given rel that will be based on the given
	 * {@link UriTemplate} but enriched with the values of the given
	 * {@link CursorPaginationSlice} (if not {@literal null}).
	 * @param base
	 * @param slice
	 * @param continuationToken
	 * @param rel must not be {@literal null} or empty.
	 * @return a {@link Link} with the given rel
	 */
	private Link createLink(UriTemplate base, CursorPaginationSlice<T> slice, String continuationToken, String rel) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUri(base.expand());
		if (StringUtils.hasText(continuationToken)) {
			builder.replaceQueryParam(CONTINUATION_TOKEN_QUERY_PARAM, continuationToken);
		}
		pageableResolver.enhance(builder, getMethodParameter(), slice);
		return Link.of(UriTemplate.of(builder.build().toString()), rel);
	}

	private Link createLink(UriTemplate base, CursorPaginationSlice<T> slice, String rel) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromUri(base.expand());
		pageableResolver.enhance(builder, getMethodParameter(), slice);
		if ("first".equals(rel)) {
			builder.replaceQueryParam(CONTINUATION_TOKEN_QUERY_PARAM, "null");
			String url = builder.build().toString().replace("&continuationToken=null", "");
			return Link.of(UriTemplate.of(url), rel);
		}
		return Link.of(UriTemplate.of(builder.build().toString()), rel);
	}

	/**
	 * Creates a new {@link SlicedModel.SliceMetadata} instance from the given
	 * {@link Slice}.
	 * @param slice must not be {@literal null}.
	 * @return
	 */
	private SlicedModel.SliceMetadata asSliceMetadata(CursorPaginationSlice<?> slice) {

		Assert.notNull(slice, "Slice must not be null!");

		return SlicedModel.SliceMetadata.of(slice.getSize(), slice.getContinuationToken());
	}

	private String baseUriOrCurrentRequest() {
		return baseUri.map(Object::toString).orElseGet(SlicedResourcesAssembler::currentRequest);
	}

	private static String currentRequest() {
		return ServletUriComponentsBuilder.fromCurrentRequest().build().toString();
	}

	/**
	 * Return the {@link MethodParameter} to be used to potentially qualify the paging and
	 * sorting request parameters to. Default implementations returns {@literal null},
	 * which means the parameters will not be qualified.
	 * @return
	 * @since 1.0
	 */
	@Nullable
	protected MethodParameter getMethodParameter() {
		return null;
	}

}
