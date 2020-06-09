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
package it.davidepedone.scp.pagination;

import lombok.EqualsAndHashCode;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A slice of data restricted by the configured ContinuationToken
 *
 * @author Davide Pedone
 * @since 1.0
 */
@EqualsAndHashCode
public class CursorPaginationSlice<T> {

	private final List<T> content = new ArrayList<>();

	private final Boolean hasNext;

	private final String continuationToken;

	private final int size;

	/**
	 * Creates a new {@link CursorPaginationSlice} with the given content and metadata
	 * @param content must not be {@literal null}. from the current one.
	 * @param size the size of the {@link CursorPaginationSlice} to be returned.
	 * @param continuationToken continuationToken to access the next
	 * {@link CursorPaginationSlice}. Can be {@literal null}.
	 */
	public CursorPaginationSlice(@NonNull List<T> content, int size, @Nullable String continuationToken) {

		Assert.notNull(content, "Content must not be null!");

		this.content.addAll(content);
		this.continuationToken = continuationToken;
		this.hasNext = continuationToken != null;
		this.size = size;
	}

	/**
	 * Returns the continuationToken to access the next {@link CursorPaginationSlice}
	 * whether there's one.
	 * @return Returns the continuationToken to access the next
	 * {@link CursorPaginationSlice} whether there's one.
	 */
	public String getContinuationToken() {
		return continuationToken;
	}

	/**
	 * Returns the number of elements currently on this {@link CursorPaginationSlice}.
	 * @return the number of elements currently on this {@link CursorPaginationSlice}.
	 */
	public int getNumberOfElements() {
		return content.size();
	}

	/**
	 * Returns if there is a next {@link CursorPaginationSlice}.
	 * @return if there is a next {@link CursorPaginationSlice}.
	 */
	public boolean hasNext() {
		return this.hasNext;
	}

	/**
	 * Returns whether the {@link CursorPaginationSlice} has content at all.
	 * @return whether the {@link CursorPaginationSlice} has content at all.
	 */
	public boolean hasContent() {
		return !content.isEmpty();
	}

	/**
	 * Returns the page content as {@link List}.
	 * @return the page content as {@link List}.
	 */
	public List<T> getContent() {
		return Collections.unmodifiableList(content);
	}

	/**
	 * Returns the size of the {@link CursorPaginationSlice}.
	 * @return the size of the {@link CursorPaginationSlice}.
	 */
	public int getSize() {
		return size;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<T> iterator() {
		return content.iterator();
	}

}
