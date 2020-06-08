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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * DTO to implement binding response representations of sliceable collections.
 *
 * @author Davide Pedone
 * @since 1.0
 */
@EqualsAndHashCode
public class SlicedModel<T> extends CollectionModel<T> {

	private final Collection<T> content;

	private SlicedModel.SliceMetadata metadata;

	/**
	 * Default constructor to allow instantiation by reflection.
	 */
	protected SlicedModel() {
		this(new ArrayList<T>(), null);
	}

	/**
	 * Creates a new {@link SlicedModel} from the given content,
	 * {@link SlicedModel.SliceMetadata} and {@link Link}s (optional).
	 * @param content must not be {@literal null}.
	 * @param metadata the metadata to add to the {@link SlicedModel}.
	 * @param links the links to add to the {@link SlicedModel}.
	 */
	protected SlicedModel(Collection<T> content, SlicedModel.SliceMetadata metadata, Link... links) {
		this(content, metadata, Arrays.asList(links));
	}

	/**
	 * Creates a new {@link SlicedModel} from the given content
	 * {@link SlicedModel.SliceMetadata} and {@link Link}s.
	 * @param content must not be {@literal null}.
	 * @param metadata the metadata to add to the {@link SlicedModel}.
	 * @param links the links to add to the {@link SlicedModel}.
	 */
	protected SlicedModel(Collection<T> content, SlicedModel.SliceMetadata metadata, Iterable<Link> links) {
		Assert.notNull(content, "Content must not be null!");

		this.content = new ArrayList<>();
		this.content.addAll(content);
		this.metadata = metadata;
		this.add(links);
	}

	/**
	 * Creates a new empty collection model.
	 * @param <T>
	 * @return
	 * @since 1.0
	 */
	public static <T> SlicedModel<T> empty() {
		return of(Collections.emptyList(), new SliceMetadata());
	}

	/**
	 * Creates a new empty collection model with the given links.
	 * @param <T>
	 * @param links must not be {@literal null}.
	 * @return
	 * @since 1.0
	 */
	public static <T> SlicedModel<T> empty(Iterable<Link> links) {
		return of(Collections.emptyList(), new SliceMetadata(), links);
	}

	/**
	 * Creates a new {@link SlicedModel} with the given content and metadata.
	 * @param content must not be {@literal null}.
	 * @param metadata the metadata to add to the {@link SlicedModel}.
	 * @return
	 * @since 1.0
	 */
	public static <T> SlicedModel<T> of(Collection<T> content, SlicedModel.SliceMetadata metadata) {
		return of(content, metadata, Collections.emptyList());
	}

	/**
	 * Creates a new {@link SlicedModel} with the given content, metadata and
	 * {@link Link}s.
	 * @param content must not be {@literal null}.
	 * @param metadata the metadata to add to the {@link SlicedModel}.
	 * @param links the links to add to the {@link SlicedModel}.
	 * @return
	 */
	public static <T> SlicedModel<T> of(Collection<T> content, SlicedModel.SliceMetadata metadata,
			Iterable<Link> links) {
		return new SlicedModel<>(content, metadata, links);
	}

	/**
	 * Returns the pagination metadata.
	 * @return the metadata
	 */
	@JsonProperty("page")
	public SlicedModel.SliceMetadata getMetadata() {
		return metadata;
	}

	/**
	 * Factory method to easily create a {@link SlicedModel} instance from a set of
	 * entities and pagination metadata.
	 * @param content must not be {@literal null}.
	 * @param metadata
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends EntityModel<S>, S> SlicedModel<T> wrap(Iterable<S> content,
			SlicedModel.SliceMetadata metadata) {

		Assert.notNull(content, "Content must not be null!");
		ArrayList<T> resources = new ArrayList<T>();

		for (S element : content) {
			resources.add((T) new EntityModel<S>(element));
		}

		return new SlicedModel<T>(resources, metadata);
	}

	/**
	 * Returns the Link pointing to the next page (if set).
	 * @return
	 */
	@JsonIgnore
	public Link getNextLink() {
		return getLink(IanaLinkRelations.NEXT).orElse(null);
	}

	/**
	 * Returns the Link pointing to the current page (if set).
	 * @return
	 */
	@JsonIgnore
	public Link getSelfLink() {
		return getLink(IanaLinkRelations.SELF).orElse(null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<T> iterator() {
		return content.iterator();
	}

	@EqualsAndHashCode
	public static class SliceMetadata {

		@JsonProperty
		private long size;

		@JsonProperty
		private Boolean hasNext;

		@JsonProperty
		private String continuationToken;

		protected SliceMetadata() {

		}

		/**
		 * Creates a new {@link SlicedModel.SliceMetadata} from the given size and
		 * continuationToken.
		 * @param size
		 * @param continuationToken
		 */
		protected SliceMetadata(long size, @Nullable String continuationToken) {
			Assert.isTrue(size > -1, "Size must not be negative!");
			this.size = size;
			this.hasNext = StringUtils.hasText(continuationToken);
			this.continuationToken = continuationToken;
		}

		public static SliceMetadata of(long size, String continuationToken) {
			return new SliceMetadata(size, continuationToken);
		}

		/**
		 * Returns the requested size of the page.
		 * @return the size a positive long.
		 */
		public long getSize() {
			return size;
		}

		public Boolean getHasNext() {
			return hasNext;
		}

		public String getContinuationToken() {
			return continuationToken;
		}

	}

}
