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
package it.davidepedone.scp.service;

import com.mongodb.MongoException;
import it.davidepedone.scp.data.CursorPageRequest;
import it.davidepedone.scp.data.CursorPageable;
import it.davidepedone.scp.exception.CursorPaginationException;
import it.davidepedone.scp.pagination.CursorPaginationSlice;
import it.davidepedone.scp.testutils.Person;
import it.davidepedone.scp.testutils.PersonPaginationService;
import it.davidepedone.scp.testutils.PersonSearchFilter;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Unit tests for {@link CursorPaginationService}.
 *
 * @author Davide Pedone
 */
@ExtendWith(MockitoExtension.class)
class CursorPaginationServiceUnitTest {

	@Mock
	private MongoOperations mongoOperations;

	@Mock
	private MappingMongoConverter mappingMongoConverter;

	@Mock
	private MongoMappingContext mappingContext;

	@BeforeEach
	void setUp() {
		initMocks(this);
		doReturn(mappingMongoConverter).when(mongoOperations).getConverter();
		doReturn(mappingContext).when(mappingMongoConverter).getMappingContext();
		BasicMongoPersistentEntity<Person> persistentEntity = new BasicMongoPersistentEntity<>(
				ClassTypeInformation.from(Person.class));
		doReturn(persistentEntity).when(mappingContext).getPersistentEntity(Person.class);

	}

	@Test
	@DisplayName("Throws an exception when sorting on unindexed field")
	void sorting() {
		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class);
		PersonSearchFilter filter = new PersonSearchFilter();
		CursorPageRequest pageRequest = CursorPageRequest.of(10, "name", Sort.Direction.ASC);
		assertThrows(IllegalArgumentException.class, () -> {
			personPaginationService.executeQuery(pageRequest, filter, null);
		});
	}

	@Test
	@DisplayName("Throws an exception when search filter doesn't match previous request")
	void hashCheck() throws Exception {
		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class) {
			@Override
			protected String getHash(CursorPageable cursorPageable, PersonSearchFilter filter) {
				return "expected";
			}
		};
		String onePart = personPaginationService.encrypt("tampered");
		PersonSearchFilter filter = new PersonSearchFilter();
		CursorPageRequest pageRequest = CursorPageRequest.of(onePart, 10);
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			personPaginationService.executeQuery(pageRequest, filter, null);
		});
		assertEquals("Can't modify search filter when using a continuationToken", thrown.getMessage());
	}

	@Test
	@DisplayName("Throws an exception when continuationToken has unexpected number of parts")
	void continuationTokenParts() throws Exception {
		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class) {
			@Override
			protected String getHash(CursorPageable cursorPageable, PersonSearchFilter filter) {
				return "justone";
			}
		};
		String onePart = personPaginationService.encrypt("justone");
		PersonSearchFilter filter = new PersonSearchFilter();
		CursorPageRequest pageRequest = CursorPageRequest.of(onePart, 10);
		CursorPaginationException thrown = assertThrows(CursorPaginationException.class, () -> {
			personPaginationService.executeQuery(pageRequest, filter, null);
		});
		assertEquals("ContinuationToken was expected to have 2 or 4 parts, but got 1", thrown.getMessage());
	}

	@Test
	@DisplayName("Throws an exception when an error occurs encrypting token")
	void encryptTokenError() {
		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class);
		CursorPaginationException thrown = assertThrows(CursorPaginationException.class, () -> {
			personPaginationService.encrypt(null);
		});
		assertEquals("Error encrypting token", thrown.getMessage());
	}

	@Test
	@DisplayName("Throws an exception when an error occurs decrypting token")
	void decryptTokenError() {
		PersonPaginationService personPaginationService2 = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class);
		CursorPaginationException thrown = assertThrows(CursorPaginationException.class, () -> {
			personPaginationService2.decrypt(null);
		});
		assertEquals("Error decrypting token", thrown.getMessage());
	}

	@Test
	@DisplayName("Throws an exception when query fails on database")
	void mongoException() {
		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class);
		doThrow(new MongoException(10, "not a real message from mongodb")).when(mongoOperations).find(any(), any());
		CursorPaginationException thrown = assertThrows(CursorPaginationException.class, () -> {
			personPaginationService.executeQuery(CursorPageRequest.of(10), new PersonSearchFilter(), null);
		});
		assertEquals("Error executing query", thrown.getMessage());
		assertTrue(thrown.getCause() instanceof MongoException);
	}

	@Test
	@DisplayName("Don't set a query timeout when queryDurationMaxTime is null")
	void timeout() {
		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class);
		ArgumentCaptor<Query> argumentCaptor = ArgumentCaptor.forClass(Query.class);
		try {
			personPaginationService.executeQuery(CursorPageRequest.of(10), new PersonSearchFilter(), null);
			verify(mongoOperations).find(argumentCaptor.capture(), eq(Person.class));
			Query executedQuery = argumentCaptor.getValue();
			assertNull(executedQuery.getMeta().getMaxTimeMsec());
		}
		catch (Exception e) {
			fail();
		}
	}

	@Test
	@DisplayName("Set a query timeout when queryDurationMaxTime is not null")
	void timeoutSet() {
		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class);
		personPaginationService.setQueryDurationMaxTime(Duration.of(1, ChronoUnit.SECONDS));
		ArgumentCaptor<Query> argumentCaptor = ArgumentCaptor.forClass(Query.class);
		try {
			personPaginationService.executeQuery(CursorPageRequest.of(10), new PersonSearchFilter(), null);
			verify(mongoOperations).find(argumentCaptor.capture(), eq(Person.class));
			Query executedQuery = argumentCaptor.getValue();
			Long timeout = executedQuery.getMeta().getMaxTimeMsec();
			assertNotNull(timeout);
			assertEquals(1000, timeout);
		}
		catch (Exception e) {
			fail();
		}
	}

	@Test
	@DisplayName("Properly configure query for first page with default sorting")
	void baseQueryFirstPage() {
		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class);
		ArgumentCaptor<Query> argumentCaptor = ArgumentCaptor.forClass(Query.class);
		try {
			personPaginationService.executeQuery(CursorPageRequest.of(1), new PersonSearchFilter(), null);
			verify(mongoOperations).find(argumentCaptor.capture(), eq(Person.class));
			Query executedQuery = argumentCaptor.getValue();
			assertEquals(2, executedQuery.getLimit());
			assertEquals(0, executedQuery.getQueryObject().keySet().size(), "No criteria added");
			Document sort = executedQuery.getSortObject();
			assertEquals(1, sort.keySet().size());
			assertNotNull(sort.get("_id"));
			assertEquals(-1, sort.get("_id"));
		}
		catch (Exception e) {
			fail();
		}
	}

	@Test
	@DisplayName("Properly configure query for first page with default sorting ascending")
	void baseQueryFirstPageAsc() {
		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class);
		ArgumentCaptor<Query> argumentCaptor = ArgumentCaptor.forClass(Query.class);
		try {
			PersonSearchFilter filter = new PersonSearchFilter();
			CursorPageRequest pageRequest = CursorPageRequest.of(1, null, Sort.Direction.ASC);
			personPaginationService.executeQuery(pageRequest, filter, null);
			verify(mongoOperations).find(argumentCaptor.capture(), eq(Person.class));
			Query executedQuery = argumentCaptor.getValue();
			assertEquals(2, executedQuery.getLimit());
			assertEquals(0, executedQuery.getQueryObject().keySet().size(), "No criteria added");
			Document sort = executedQuery.getSortObject();
			assertEquals(1, sort.keySet().size());
			assertNotNull(sort.get("_id"));
			assertEquals(1, sort.get("_id"));
		}
		catch (Exception e) {
			fail();
		}
	}

	@Test
	@DisplayName("Properly configure query for nth page with default sorting")
	void baseQueryNthPageDesc() {
		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class) {
			@Override
			protected String getHash(CursorPageable cursorPageable, PersonSearchFilter filter) {
				return "prevhash";
			}
		};
		ArgumentCaptor<Query> argumentCaptor = ArgumentCaptor.forClass(Query.class);
		try {
			PersonSearchFilter filter = new PersonSearchFilter();
			ObjectId anObjectId = new ObjectId();
			String continuationToken = String.format("prevhash_%s", anObjectId);
			CursorPageRequest pageRequest = CursorPageRequest.of(personPaginationService.encrypt(continuationToken), 1);
			personPaginationService.executeQuery(pageRequest, filter, null);
			verify(mongoOperations).find(argumentCaptor.capture(), eq(Person.class));
			Query executedQuery = argumentCaptor.getValue();
			assertEquals(2, executedQuery.getLimit());
			assertEquals(1, executedQuery.getQueryObject().keySet().size(), "No criteria was added");
			Document _idCriteria = (Document) executedQuery.getQueryObject().get("_id");
			assertEquals(1, _idCriteria.keySet().size());
			assertEquals(anObjectId, _idCriteria.get("$lt"));
			Document sort = executedQuery.getSortObject();
			assertEquals(1, sort.keySet().size());
			assertNotNull(sort.get("_id"));
			assertEquals(-1, sort.get("_id"));
		}
		catch (Exception e) {
			fail();
		}
	}

	@Test
	@DisplayName("Properly configure query for nth page with default sorting ascending")
	void baseQueryNthPageAsc() {
		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class) {
			@Override
			protected String getHash(CursorPageable cursorPageable, PersonSearchFilter filter) {
				return "prevhash";
			}
		};
		ArgumentCaptor<Query> argumentCaptor = ArgumentCaptor.forClass(Query.class);
		try {
			PersonSearchFilter filter = new PersonSearchFilter();
			ObjectId anObjectId = new ObjectId();
			String continuationToken = String.format("prevhash_%s", anObjectId);
			CursorPageRequest pageRequest = CursorPageRequest.of(personPaginationService.encrypt(continuationToken), 1,
					null, Sort.Direction.ASC);
			personPaginationService.executeQuery(pageRequest, filter, null);
			verify(mongoOperations).find(argumentCaptor.capture(), eq(Person.class));
			Query executedQuery = argumentCaptor.getValue();
			assertEquals(2, executedQuery.getLimit());
			assertEquals(1, executedQuery.getQueryObject().keySet().size(), "No criteria was added");
			Document _idCriteria = (Document) executedQuery.getQueryObject().get("_id");
			assertEquals(1, _idCriteria.keySet().size());
			assertEquals(anObjectId, _idCriteria.get("$gt"));
			Document sort = executedQuery.getSortObject();
			assertEquals(1, sort.keySet().size());
			assertNotNull(sort.get("_id"));
			assertEquals(1, sort.get("_id"));
		}
		catch (Exception e) {
			fail();
		}
	}

	@Test
	@DisplayName("Properly configure query for first page with custom sorting ascending")
	void queryFirstPageWithSortingAsc() {
		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class);
		ArgumentCaptor<Query> argumentCaptor = ArgumentCaptor.forClass(Query.class);
		try {
			PersonSearchFilter filter = new PersonSearchFilter();
			CursorPageRequest pageRequest = CursorPageRequest.of(1, "age", Sort.Direction.ASC);
			personPaginationService.executeQuery(pageRequest, filter, null);
			verify(mongoOperations).find(argumentCaptor.capture(), eq(Person.class));
			Query executedQuery = argumentCaptor.getValue();
			assertEquals(2, executedQuery.getLimit());
			assertEquals(0, executedQuery.getQueryObject().keySet().size(), "No criteria added");
			Document sort = executedQuery.getSortObject();
			assertEquals(2, sort.keySet().size());
			assertNotNull(sort.get("_id"));
			assertEquals(1, sort.get("_id"));
			assertNotNull(sort.get("age"));
			assertEquals(1, sort.get("age"));
		}
		catch (Exception e) {
			fail();
		}
	}

	@Test
	@DisplayName("Properly configure query for first page with custom sorting descending")
	void queryFirstPageWithSortingDesc() {
		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class);
		ArgumentCaptor<Query> argumentCaptor = ArgumentCaptor.forClass(Query.class);
		try {
			PersonSearchFilter filter = new PersonSearchFilter();
			CursorPageRequest pageRequest = CursorPageRequest.of(1, "age", Sort.Direction.DESC);
			personPaginationService.executeQuery(pageRequest, filter, null);
			verify(mongoOperations).find(argumentCaptor.capture(), eq(Person.class));
			Query executedQuery = argumentCaptor.getValue();
			assertEquals(2, executedQuery.getLimit());
			assertEquals(0, executedQuery.getQueryObject().keySet().size(), "No criteria added");
			Document sort = executedQuery.getSortObject();
			assertEquals(2, sort.keySet().size());
			assertNotNull(sort.get("_id"));
			assertEquals(-1, sort.get("_id"));
			assertNotNull(sort.get("age"));
			assertEquals(-1, sort.get("age"));
		}
		catch (Exception e) {
			fail();
		}
	}

	@Test
	@DisplayName("Properly configure query for nth page with custom sorting ascending")
	void queryNthPageWithSortingAsc() {
		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class) {
			@Override
			protected String getHash(CursorPageable cursorPageable, PersonSearchFilter filter) {
				return "prevhash";
			}
		};
		ArgumentCaptor<Query> argumentCaptor = ArgumentCaptor.forClass(Query.class);
		doReturn(Long.valueOf("10")).when(mappingMongoConverter)
			.convertToMongoType((Object) any(), (TypeInformation<?>) any());
		try {
			PersonSearchFilter filter = new PersonSearchFilter();
			ObjectId anObjectId = new ObjectId();
			String continuationToken = String.format("prevhash_%s_age_10", anObjectId);
			CursorPageRequest pageRequest = CursorPageRequest.of(personPaginationService.encrypt(continuationToken), 1,
					"age", Sort.Direction.ASC);

			personPaginationService.executeQuery(pageRequest, filter, null);
			verify(mongoOperations).find(argumentCaptor.capture(), eq(Person.class));
			Query executedQuery = argumentCaptor.getValue();
			assertEquals(2, executedQuery.getLimit());
			assertEquals(1, executedQuery.getQueryObject().keySet().size(), "No criteria was added");

			List<Document> orCriteria = (List<Document>) executedQuery.getQueryObject().get("$or");
			assertEquals(2, orCriteria.size());
			Document tieBreaker = new Document();
			tieBreaker.put("age", Long.valueOf(10));
			tieBreaker.put("_id", new Document("$gt", anObjectId));
			Document age = new Document("age", new Document("$gt", Long.valueOf(10)));
			assertThat(orCriteria).isEqualTo(Arrays.asList(tieBreaker, age));
			Document sort = executedQuery.getSortObject();
			assertEquals(2, sort.keySet().size());
			assertNotNull(sort.get("_id"));
			assertEquals(1, sort.get("_id"));
			assertNotNull(sort.get("age"));
			assertEquals(1, sort.get("age"));
		}
		catch (Exception e) {
			fail();
		}
	}

	@Test
	@DisplayName("Properly configure query for nth page with custom sorting descending")
	void queryNthPageWithSortingDesc() {
		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class) {
			@Override
			protected String getHash(CursorPageable cursorPageable, PersonSearchFilter filter) {
				return "prevhash";
			}
		};
		ArgumentCaptor<Query> argumentCaptor = ArgumentCaptor.forClass(Query.class);
		doReturn(Long.valueOf("10")).when(mappingMongoConverter)
			.convertToMongoType((Object) any(), (TypeInformation<?>) any());
		try {
			PersonSearchFilter filter = new PersonSearchFilter();
			ObjectId anObjectId = new ObjectId();
			String continuationToken = String.format("prevhash_%s_age_10", anObjectId);
			CursorPageRequest pageRequest = CursorPageRequest.of(personPaginationService.encrypt(continuationToken), 1,
					"age", Sort.Direction.DESC);

			personPaginationService.executeQuery(pageRequest, filter, null);
			verify(mongoOperations).find(argumentCaptor.capture(), eq(Person.class));
			Query executedQuery = argumentCaptor.getValue();
			assertEquals(2, executedQuery.getLimit());
			assertEquals(1, executedQuery.getQueryObject().keySet().size(), "No criteria was added");

			List<Document> orCriteria = (List<Document>) executedQuery.getQueryObject().get("$or");
			assertEquals(2, orCriteria.size());
			Document tieBreaker = new Document();
			tieBreaker.put("age", Long.valueOf(10));
			tieBreaker.put("_id", new Document("$lt", anObjectId));
			Document age = new Document("age", new Document("$lt", Long.valueOf(10)));
			assertThat(orCriteria).isEqualTo(Arrays.asList(tieBreaker, age));
			Document sort = executedQuery.getSortObject();
			assertEquals(2, sort.keySet().size());
			assertNotNull(sort.get("_id"));
			assertEquals(-1, sort.get("_id"));
			assertNotNull(sort.get("age"));
			assertEquals(-1, sort.get("age"));
		}
		catch (Exception e) {
			fail();
		}
	}

	@Test
	@DisplayName("Return proper ContinuationTokenSlice for page with default sorting")
	void baseQueryResponse() {
		Person p1 = new Person();
		p1.setId(new ObjectId().toString());
		p1.setAge(10);
		p1.setBirthday(new Date());
		p1.setName("Davide");

		Person p2 = new Person();
		p2.setId(new ObjectId().toString());
		p2.setAge(20);
		p2.setBirthday(new Date());
		p2.setName("Franco");

		Person p3 = new Person();
		p3.setId(new ObjectId().toString());
		p3.setAge(20);
		p3.setBirthday(new Date());
		p3.setName("Gianni");

		doReturn(Arrays.asList(p1, p2, p3)).when(mongoOperations).find(any(), any());

		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class) {
			@Override
			protected String getHash(CursorPageable cursorPageable, PersonSearchFilter filter) {
				return "mockhash";
			}

			@Override
			protected Object getValue(MongoPersistentProperty prop, Person entity) throws CursorPaginationException {
				return "mockvalue";
			}
		};
		try {
			PersonSearchFilter filter = new PersonSearchFilter();
			CursorPageRequest pageRequest = CursorPageRequest.of(2);
			CursorPaginationSlice<Person> slice = personPaginationService.executeQuery(pageRequest, filter, null);
			assertNotNull(slice);
			assertNotNull(slice.getContinuationToken());
			assertTrue(slice.hasNext());
			assertTrue(slice.hasContent());
			assertEquals(2, slice.getContent().size());
			String decodedToken = personPaginationService.decrypt(slice.getContinuationToken());
			assertEquals(2, decodedToken.split("_").length);
			assertEquals("mockhash_mockvalue", decodedToken);
		}
		catch (Exception e) {
			fail();
		}
	}

	@Test
	@DisplayName("Return proper ContinuationTokenSlice for page with custom sorting")
	void sortedQueryResponse() {
		Person p1 = new Person();
		p1.setId(new ObjectId().toString());
		p1.setAge(10);
		p1.setBirthday(new Date());
		p1.setName("Davide");

		Person p2 = new Person();
		p2.setId(new ObjectId().toString());
		p2.setAge(20);
		p2.setBirthday(new Date());
		p2.setName("Franco");

		Person p3 = new Person();
		p3.setId(new ObjectId().toString());
		p3.setAge(20);
		p3.setBirthday(new Date());
		p3.setName("Gianni");

		doReturn(Arrays.asList(p1, p2, p3)).when(mongoOperations).find(any(), any());

		PersonPaginationService personPaginationService = new PersonPaginationService(mongoOperations,
				Arrays.asList("birhday", "age"), Person.class) {
			@Override
			protected String getHash(CursorPageable cursorPageable, PersonSearchFilter filter) {
				return "mockhash";
			}

			@Override
			protected Object getValue(MongoPersistentProperty prop, Person entity) throws CursorPaginationException {
				return "mockvalue";
			}
		};
		try {
			PersonSearchFilter filter = new PersonSearchFilter();
			CursorPageRequest pageRequest = CursorPageRequest.of(2, "age", Sort.Direction.DESC);
			CursorPaginationSlice<Person> slice = personPaginationService.executeQuery(pageRequest, filter, null);
			assertNotNull(slice);
			assertNotNull(slice.getContinuationToken());
			assertTrue(slice.hasNext());
			assertTrue(slice.hasContent());
			assertEquals(2, slice.getContent().size());
			String decodedToken = personPaginationService.decrypt(slice.getContinuationToken());
			assertEquals(4, decodedToken.split("_").length);
			assertEquals("mockhash_mockvalue_age_mockvalue", decodedToken);
		}
		catch (Exception e) {
			fail();
		}
	}

}