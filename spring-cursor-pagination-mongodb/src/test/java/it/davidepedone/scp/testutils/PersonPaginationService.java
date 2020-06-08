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
package it.davidepedone.scp.testutils;

import it.davidepedone.scp.service.CursorPaginationService;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * {@link CursorPaginationService} for test.
 *
 * @author Davide Pedone
 */
public class PersonPaginationService extends CursorPaginationService<Person, PersonSearchFilter> {

	public PersonPaginationService(MongoOperations mongoOperations, List sortableFields, String encryptionKey,
			Class aClass) {
		super(mongoOperations, sortableFields, encryptionKey, aClass);
	}

	public PersonPaginationService(MongoOperations mongoOperations, List<String> sortableFields, String encryptionKey,
			Class<Person> personClass, Duration queryDurationMaxTime) {
		super(mongoOperations, sortableFields, encryptionKey, personClass, queryDurationMaxTime);
	}

	@Override
	public void configSearchQuery(Query query, PersonSearchFilter filter) {
		Optional.ofNullable(filter.getAge()).ifPresent(age -> query.addCriteria(where("age").is(age)));
		Optional.ofNullable(filter.getName()).ifPresent(name -> query.addCriteria(where("name").is(name)));
	}

}
