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
package it.davidepedone.scp.data;

import lombok.Data;
import org.springframework.data.domain.Sort;

/**
 * Basic Java Bean implementation of {@code CursorPageable}.
 *
 * @author Davide Pedone
 * @since 1.1
 */
@Data
public class CursorPageRequest implements CursorPageable {

	private String continuationToken;

	private int size;

	private String sort;

	private Sort.Direction direction;

	protected CursorPageRequest() {
	}

	protected CursorPageRequest(String continuationToken, int size, String sort, Sort.Direction direction) {
		this.continuationToken = continuationToken;
		this.size = size;
		this.sort = sort;
		this.direction = direction;
	}

	public static CursorPageRequest of(String continuationToken, int size) {
		return of(continuationToken, size, null, CursorPageable.unpaged().getDirection());
	}

	public static CursorPageRequest of(int size) {
		return of(null, size, null, CursorPageable.unpaged().getDirection());
	}

	public static CursorPageRequest of(int size, String sort, Sort.Direction direction) {
		return of(null, size, sort, direction);
	}

	public static CursorPageRequest of(String continuationToken, int size, String sort) {
		return of(continuationToken, size, sort, CursorPageable.unpaged().getDirection());
	}

	public static CursorPageRequest of(String continuationToken, int size, String sort, Sort.Direction direction) {
		return new CursorPageRequest(continuationToken, size, sort, direction);
	}

}
