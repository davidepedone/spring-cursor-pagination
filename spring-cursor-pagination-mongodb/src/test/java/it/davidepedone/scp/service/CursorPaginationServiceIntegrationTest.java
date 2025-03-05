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

import it.davidepedone.scp.data.CursorPageRequest;
import it.davidepedone.scp.exception.CursorPaginationException;
import it.davidepedone.scp.pagination.CursorPaginationSlice;
import it.davidepedone.scp.testutils.Person;
import it.davidepedone.scp.testutils.PersonPaginationService;
import it.davidepedone.scp.testutils.PersonSearchFilter;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link CursorPaginationService}.
 *
 * @author Davide Pedone
 */
@DataMongoTest()
@ExtendWith(SpringExtension.class)
class CursorPaginationServiceIntegrationTest {

	@TestConfiguration
	static class Config {

		@Bean
		public PersonPaginationService personPaginationService(MongoOperations mongoOperations) {
			return new PersonPaginationService(mongoOperations, Arrays.asList("name", "birthday", "age", "timestamp",
					"noGetterField", "exceptionGetterField", "nullField", "unknown"), Person.class);
		}

	}

	@Autowired
	private MongoOperations mongoOperations;

	@Autowired
	private PersonPaginationService personPaginationService;

	@BeforeEach
	void setUp() {
		mongoOperations.remove(new Query(), Person.class);
		Person p1 = new Person();
		p1.setId("5ede59ba2ed62b0006870000");
		p1.setAge(10);
		p1.setBirthday(new Date(946684800000L)); // 2000/1/1
		p1.setName("Davide");
		p1.setTimestamp(1561932000000L);
		p1.setNoGetterField("randomstring");

		Person p2 = new Person();
		p2.setId("5ede59ba2ed62b0006874000");
		p2.setAge(20);
		p2.setBirthday(new Date(978307200000L)); // 2001/1/1
		p2.setName("Franco");
		p2.setTimestamp(1591631632000L);
		p2.setNoGetterField("randomstring");

		Person p3 = new Person();
		p3.setId("5ede59ba2ed62b0006871000");
		p3.setAge(20);
		p3.setBirthday(new Date(1577836800000L)); // 2020/1/1
		p3.setName("Gianni");
		p3.setNoGetterField("randomstring");

		Person p4 = new Person();
		p4.setId("5ede59ba2ed62b0006872000");
		p4.setName("Mario");
		p4.setNoGetterField("randomstring");

		mongoOperations.insert(Arrays.asList(p1, p2, p3, p4), Person.class);
	}

	@Test
	void sortingByIdAsc() throws Exception {
		PersonSearchFilter filter = new PersonSearchFilter();
		CursorPageRequest pageRequest = CursorPageRequest.of(3, null, Sort.Direction.ASC);
		CursorPaginationSlice<Person> slice1 = personPaginationService.executeQuery(pageRequest, filter, null);
		assertNotNull(slice1.getContinuationToken());
		assertTrue(slice1.hasContent());
		assertEquals(3, slice1.getContent().size());
		assertEquals(3, slice1.getSize());
		assertEquals(3, slice1.getNumberOfElements());
		assertTrue(slice1.hasNext());
		// get NextPage
		pageRequest.setContinuationToken(slice1.getContinuationToken());
		CursorPaginationSlice<Person> slice2 = personPaginationService.executeQuery(pageRequest, filter, null);
		assertNull(slice2.getContinuationToken());
		assertTrue(slice2.hasContent());
		assertEquals(1, slice2.getContent().size());
		assertEquals(3, slice2.getSize());
		assertEquals(1, slice2.getNumberOfElements());
		assertFalse(slice2.hasNext());
		assertEquals("5ede59ba2ed62b0006870000", slice1.getContent().get(0).getId());
		assertEquals("5ede59ba2ed62b0006871000", slice1.getContent().get(1).getId());
		assertEquals("5ede59ba2ed62b0006872000", slice1.getContent().get(2).getId());
		assertEquals("5ede59ba2ed62b0006874000", slice2.getContent().get(0).getId());
	}

	@Test
	void sortingByIdDesc() throws Exception {
		PersonSearchFilter filter = new PersonSearchFilter();
		CursorPageRequest pageRequest = CursorPageRequest.of(3);
		CursorPaginationSlice<Person> slice1 = personPaginationService.executeQuery(pageRequest, filter, null);
		assertNotNull(slice1.getContinuationToken());
		assertTrue(slice1.hasContent());
		assertEquals(3, slice1.getContent().size());
		assertEquals(3, slice1.getSize());
		assertEquals(3, slice1.getNumberOfElements());
		assertTrue(slice1.hasNext());
		// get NextPage
		pageRequest.setContinuationToken(slice1.getContinuationToken());
		CursorPaginationSlice<Person> slice2 = personPaginationService.executeQuery(pageRequest, filter, null);
		assertNull(slice2.getContinuationToken());
		assertTrue(slice2.hasContent());
		assertEquals(1, slice2.getContent().size());
		assertEquals(3, slice2.getSize());
		assertEquals(1, slice2.getNumberOfElements());
		assertFalse(slice2.hasNext());
		assertEquals("5ede59ba2ed62b0006874000", slice1.getContent().get(0).getId());
		assertEquals("5ede59ba2ed62b0006872000", slice1.getContent().get(1).getId());
		assertEquals("5ede59ba2ed62b0006871000", slice1.getContent().get(2).getId());
		assertEquals("5ede59ba2ed62b0006870000", slice2.getContent().get(0).getId());
	}

	@Test
	void sortByDateAsc() throws Exception {
		PersonSearchFilter filter = new PersonSearchFilter();
		CursorPageRequest pageRequest = CursorPageRequest.of(3, "birthday", Sort.Direction.ASC);
		CursorPaginationSlice<Person> slice1 = personPaginationService.executeQuery(pageRequest, filter, null);
		assertNotNull(slice1.getContinuationToken());
		assertTrue(slice1.hasContent());
		assertEquals(3, slice1.getContent().size());
		assertEquals(3, slice1.getSize());
		assertEquals(3, slice1.getNumberOfElements());
		assertTrue(slice1.hasNext());
		// get NextPage
		pageRequest.setContinuationToken(slice1.getContinuationToken());
		CursorPaginationSlice<Person> slice2 = personPaginationService.executeQuery(pageRequest, filter, null);
		assertNull(slice2.getContinuationToken());
		assertTrue(slice2.hasContent());
		assertEquals(1, slice2.getContent().size());
		assertEquals(3, slice2.getSize());
		assertEquals(1, slice2.getNumberOfElements());
		assertFalse(slice2.hasNext());

		assertEquals("5ede59ba2ed62b0006872000", slice1.getContent().get(0).getId());
		assertEquals("5ede59ba2ed62b0006870000", slice1.getContent().get(1).getId());
		assertEquals("5ede59ba2ed62b0006874000", slice1.getContent().get(2).getId());
		assertEquals("5ede59ba2ed62b0006871000", slice2.getContent().get(0).getId());
	}

	@Test
	@DisplayName("These test demonstrates how sorting by a field containing null values may produce unexpected results on edge cases")
	void sortByDateDesc() throws Exception {
		PersonSearchFilter filter = new PersonSearchFilter();
		CursorPageRequest pageRequest = CursorPageRequest.of(3, "birthday", Sort.Direction.DESC);
		CursorPaginationSlice<Person> slice1 = personPaginationService.executeQuery(pageRequest, filter, null);
		assertNotNull(slice1.getContinuationToken());
		assertTrue(slice1.hasContent());
		assertEquals(3, slice1.getContent().size());
		assertEquals(3, slice1.getSize());
		assertEquals(3, slice1.getNumberOfElements());
		assertTrue(slice1.hasNext());
		assertEquals("5ede59ba2ed62b0006871000", slice1.getContent().get(0).getId());
		assertEquals("5ede59ba2ed62b0006874000", slice1.getContent().get(1).getId());
		assertEquals("5ede59ba2ed62b0006870000", slice1.getContent().get(2).getId());
		// get NextPage
		pageRequest.setContinuationToken(slice1.getContinuationToken());
		CursorPaginationSlice<Person> slice2 = personPaginationService.executeQuery(pageRequest, filter, null);
		assertNull(slice2.getContinuationToken());
		/*
		 * we know that there is a next page, trying to get it using produced Query: {
		 * "$or" : [{ "birthday" : { "$date" : "2000-01-01T00:00:00Z"}, "_id" : { "$lt" :
		 * { "$oid" : "5ede59ba2ed62b0006870000"}}}, { "birthday" : { "$lt" : { "$date" :
		 * "2000-01-01T00:00:00Z"}}}]}, Sort: { "birthday" : -1, "_id" : -1} do not return
		 * any results
		 */
	}

	@Test
	@DisplayName("Throws an exception when reflection fails for missing getter")
	void reflectionNoGetterNoParty() {
		PersonSearchFilter filter = new PersonSearchFilter();
		CursorPageRequest pageRequest = CursorPageRequest.of(3, "noGetterField", Sort.Direction.DESC);
		CursorPaginationException thrown = assertThrows(CursorPaginationException.class, () -> {
			personPaginationService.executeQuery(pageRequest, filter, null);
		});
		assertEquals("No getter found for property noGetterField", thrown.getMessage());
	}

	@Test
	@DisplayName("Throws an exception when reflection invocation throws an exception")
	void reflectionException() {
		PersonSearchFilter filter = new PersonSearchFilter();
		CursorPageRequest pageRequest = CursorPageRequest.of(3, "exceptionGetterField", Sort.Direction.DESC);
		CursorPaginationException thrown = assertThrows(CursorPaginationException.class, () -> {
			personPaginationService.executeQuery(pageRequest, filter, null);
		});
		assertEquals("Error invoking getter getExceptionGetterField for property exceptionGetterField",
				thrown.getMessage());
	}

	@Test
	@DisplayName("Throws an exception when building continuationToken sorting field value is null")
	void sortWithNull() {
		PersonSearchFilter filter = new PersonSearchFilter();
		CursorPageRequest pageRequest = CursorPageRequest.of(3, "nullField", Sort.Direction.ASC);
		CursorPaginationException thrown = assertThrows(CursorPaginationException.class, () -> {
			personPaginationService.executeQuery(pageRequest, filter, null);
		});
		assertEquals("Null value not allowed for property nullField", thrown.getMessage());
	}

	@Test
	@DisplayName("Throws an exception when property from decoded token doesn't exists")
	void unknownPropertyDecrypting() throws Exception {
		PersonSearchFilter filter = new PersonSearchFilter();
		String continuationToken = String.format("18f64466798e56fa1fc8a9c729468e18_%s_unknownProperty_10",
				new ObjectId());
		CursorPageRequest pageRequest = CursorPageRequest.of(personPaginationService.encrypt(continuationToken), 10);
		CursorPaginationException thrown = assertThrows(CursorPaginationException.class, () -> {
			personPaginationService.executeQuery(pageRequest, filter, null);
		});
		assertEquals("Error getting parameter value: null", thrown.getMessage());
	}

	@Test
	@DisplayName("Throws an exception when property for encoding token doesn't exists")
	void unknownPropertyEncrypting() {
		PersonSearchFilter filter = new PersonSearchFilter();
		CursorPageRequest pageRequest = CursorPageRequest.of(3, "unknown", Sort.Direction.ASC);
		CursorPaginationException thrown = assertThrows(CursorPaginationException.class, () -> {
			personPaginationService.executeQuery(pageRequest, filter, null);
		});
		assertEquals("PersistentProperty is null", thrown.getMessage());
	}

}