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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SlicedModel}.
 *
 * @author Davide Pedone
 */
class SlicedModelUnitTest {

	static final SlicedModel.SliceMetadata metadata = new SlicedModel.SliceMetadata(10, "notarealtoken");

	SlicedModel<Object> resources;

	@BeforeEach
	void setUp() {
		resources = SlicedModel.of(Collections.emptyList(), metadata);
	}

	@Test
	void discoversNextLink() {

		resources.add(Link.of("foo", IanaLinkRelations.NEXT.value()));

		assertThat(resources.getNextLink()).isNotNull();
	}

	@Test
	void discoversSelfLink() {

		resources.add(Link.of("custom", IanaLinkRelations.SELF.value()));

		assertThat(resources.getSelfLink()).isNotNull();
	}

	@Test
	void preventsNegativePageSize() {
		assertThatIllegalArgumentException().isThrownBy(() -> {
			new SlicedModel.SliceMetadata(-1, null);
		});
	}

	@Test
	void calculatesHasNextCorrectly() {
		assertTrue(resources.getMetadata().getHasNext());
		assertFalse(new SlicedModel.SliceMetadata(10, null).getHasNext());
	}

}