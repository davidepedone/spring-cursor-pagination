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

import it.davidepedone.scp.exception.CursorPaginationException;
import it.davidepedone.scp.pagination.CursorPaginationSlice;
import it.davidepedone.scp.search.CursorPaginationSearchFilter;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * @author Davide Pedone
 * @since 1.0
 */
@Slf4j
public abstract class CursorPaginationService<T, V extends CursorPaginationSearchFilter> {

	private final MongoOperations mongoOperations;

	private final List<String> sortableFields;

	private final Duration queryDurationMaxTime;

	private final String encryptionKey;

	private final ClassTypeInformation<T> classTypeInformation;

	private final Class<T> tClass;

	private final MongoPersistentEntity<?> persistentEntity;

	public CursorPaginationService(MongoOperations mongoOperations, List<String> sortableFields, String encryptionKey,
			Class<T> tClass) {
		this.mongoOperations = mongoOperations;
		this.sortableFields = sortableFields;
		this.encryptionKey = encryptionKey;
		this.queryDurationMaxTime = null;
		this.tClass = tClass;
		this.classTypeInformation = ClassTypeInformation.from(tClass);
		this.persistentEntity = mongoOperations.getConverter().getMappingContext().getPersistentEntity(tClass);
		Assert.notNull(this.persistentEntity, "PersistentEntity must not be null!");
	}

	public CursorPaginationService(MongoOperations mongoOperations, List<String> sortableFields, String encryptionKey,
			Class<T> tClass, Duration queryDurationMaxTime) {
		this.mongoOperations = mongoOperations;
		this.sortableFields = sortableFields;
		this.queryDurationMaxTime = queryDurationMaxTime;
		this.encryptionKey = encryptionKey;
		this.tClass = tClass;
		this.classTypeInformation = ClassTypeInformation.from(tClass);
		this.persistentEntity = mongoOperations.getConverter().getMappingContext().getPersistentEntity(tClass);
		Assert.notNull(this.persistentEntity, "PersistentEntity must not be null!");
	}

	public CursorPaginationSlice<T> executeQuery(V filter) throws IllegalArgumentException, CursorPaginationException {

		boolean isSorted = StringUtils.hasText(filter.getSort());

		// Prevent sorting on unindexed fields
		if (isSorted && !sortableFields.contains(filter.getSort())) {
			throw new IllegalArgumentException("Sorting is only allowed on fields: " + sortableFields);
		}

		Query query = new Query();
		Optional.ofNullable(queryDurationMaxTime).ifPresent(query::maxTime);
		configSearchQuery(query, filter);

		// calculate request filter hash ignoring page size
		String hashed = getHash(filter);

		if (StringUtils.hasText(filter.getContinuationToken())) {
			String decoded = decrypt(filter.getContinuationToken(), encryptionKey);
			log.debug("Decoded continuationToken: {}", decoded);
			String[] params = decoded.split("_");

			String prevHash = params[0];

			if (!hashed.equals(prevHash)) {
				throw new IllegalArgumentException("Can't modify search filter when using a continuationToken");
			}

			if (params.length != 2 && params.length != 4) {
				log.error("Unexpected continuationToken length: {}", params.length);
				throw new CursorPaginationException(
						"ContinuationToken was expected to have 2 or 4 parts, but got " + params.length);
			}

			if (params.length == 2) {
				// default sorting by id
				String id = params[1];
				if (Sort.Direction.DESC.equals(filter.getDirection())) {
					query.addCriteria(where("_id").lt(new ObjectId(id)));
				}
				else {
					query.addCriteria(where("_id").gt(new ObjectId(id)));
				}
			}
			else {
				String id = params[1];
				String paramName = params[2];
				String paramValueAsString = params[3];

				TypeInformation<?> typeInformation = classTypeInformation.getProperty(paramName);

				Object paramValue = "null".equals(paramValueAsString) ? null
						: mongoOperations.getConverter().convertToMongoType(paramValueAsString, typeInformation);

				Criteria criteria = new Criteria();
				List<Criteria> or = new ArrayList<>();
				if (Sort.Direction.DESC.equals(filter.getDirection())) {
					or.add(where(paramName).is(paramValue).and("_id").lt(new ObjectId(id)));
					Optional.ofNullable(paramValue).ifPresent(v -> or.add(where(paramName).lt(v)));
				}
				else {
					// TODO test
					or.add(where(paramName).is(paramValue).and("_id").gt(new ObjectId(id)));
					Optional.ofNullable(paramValue).ifPresent(v -> or.add(where(paramName).gt(v)));
				}
				query.addCriteria(criteria.orOperator(or.toArray(new Criteria[0])));
			}
		}
		query.limit(filter.getSize() + 1);
		Sort querySorting = isSorted
				? Sort.by(filter.getDirection(), filter.getSort()).and(Sort.by(filter.getDirection(), "_id"))
				: Sort.by(filter.getDirection(), "_id");
		query.with(querySorting);

		log.debug("Executing query: {}", query.toString());

		List<T> entities;
		try {
			entities = mongoOperations.find(query, tClass);
		}
		catch (Exception e) {
			// Wrap db exception
			throw new CursorPaginationException("Error executing query", e);
		}

		boolean hasNext = entities.size() > filter.getSize();
		String continuationToken = null;
		List<T> toReturn = new ArrayList<>(entities);

		if (hasNext) {
			toReturn = entities.subList(0, filter.getSize());
			T last = toReturn.get(toReturn.size() - 1);
			String plainToken = hashed + "_" + getValue(persistentEntity.getIdProperty(), last);
			if (isSorted) {
				Object sortValue = getValue(persistentEntity.getPersistentProperty(filter.getSort()), last);
				plainToken += "_" + filter.getSort() + "_" + sortValue;
			}
			log.trace("Token: {}", plainToken);
			continuationToken = encrypt(plainToken, encryptionKey);
		}

		return new CursorPaginationSlice<>(toReturn, filter.getSize(), continuationToken);
	}

	protected String getHash(V filter) {
		return DigestUtils
				.md5DigestAsHex((filter.toString() + filter.getSort() + filter.getDirection().name()).getBytes());
	}

	protected Object getValue(MongoPersistentProperty prop, T entity) throws CursorPaginationException {
		try {
			return Objects.requireNonNull(prop.getGetter()).invoke(entity);
		}
		catch (Exception e) {
			throw new CursorPaginationException("Error getting value for property " + prop.getFieldName(), e);
		}
	}

	protected String encrypt(String strToEncrypt, String secret) throws CursorPaginationException {
		try {
			SecretKeySpec skeyspec = new SecretKeySpec(secret.getBytes(), "Blowfish");
			Cipher cipher = Cipher.getInstance("Blowfish");
			cipher.init(Cipher.ENCRYPT_MODE, skeyspec);
			byte[] encrypted = cipher.doFinal(strToEncrypt.getBytes());
			return Base64Utils.encodeToUrlSafeString(encrypted);
		}
		catch (Exception e) {
			log.error("Error encrypting continuationToken: {}", e.getMessage());
			throw new CursorPaginationException("Error encrypting token", e);
		}
	}

	protected String decrypt(String strToDecrypt, String secret) throws CursorPaginationException {
		try {
			SecretKeySpec skeyspec = new SecretKeySpec(secret.getBytes(), "Blowfish");
			Cipher cipher = Cipher.getInstance("Blowfish");
			cipher.init(Cipher.DECRYPT_MODE, skeyspec);
			byte[] decrypted = cipher.doFinal(Base64Utils.decodeFromUrlSafeString(strToDecrypt));
			return new String(decrypted);
		}
		catch (Exception e) {
			log.error("Error decrypting continuationToken: {}", e.getMessage());
			throw new CursorPaginationException("Error decrypting token", e);
		}
	}

	abstract public void configSearchQuery(Query query, V filter);

}
