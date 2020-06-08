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
package it.davidepedone.scp.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.data.domain.Sort.Direction.DESC;

/**
 * Unit test for {@link CursorPaginationSearchFilter}
 *
 * @author Davide Pedone
 */
class CursorPaginationSearchFilterUnitTest {

	@Test
	@DisplayName("Provide a default size and sorting direction")
	void defaultValues() {
		CursorPaginationSearchFilter searchFilter = new CursorPaginationSearchFilter() {
		};
		assertEquals(1, searchFilter.getSize(), "Default size");
		assertEquals(DESC, searchFilter.getDirection(), "Default direction");
	}

}