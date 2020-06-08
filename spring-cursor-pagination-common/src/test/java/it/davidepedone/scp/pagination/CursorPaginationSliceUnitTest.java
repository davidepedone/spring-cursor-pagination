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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for {@link CursorPaginationSlice}
 *
 * @author Davide Pedone
 */
class CursorPaginationSliceUnitTest {

	@Test
	@DisplayName("Reject null content")
	void rejectNullContent() {
		assertThrows(IllegalArgumentException.class, () -> {
			new CursorPaginationSlice<>(null, 0, "notarealtoken");
		});
	}

	@Test
	@DisplayName("Provide proper information when continuationToken is null")
	void handleNullToken() {
		CursorPaginationSlice<String> cursorPaginationSlice = new CursorPaginationSlice<>(Arrays.asList("1", "2", "3"),
				10, null);
		assertNull(cursorPaginationSlice.getContinuationToken());
		assertEquals(3, cursorPaginationSlice.getNumberOfElements());
		assertFalse(cursorPaginationSlice.hasNext());
		assertTrue(cursorPaginationSlice.hasContent());
		assertEquals(Arrays.asList("1", "2", "3"), cursorPaginationSlice.getContent());
		assertEquals(10, cursorPaginationSlice.getSize());
		assertNotNull(cursorPaginationSlice.iterator());
	}

}