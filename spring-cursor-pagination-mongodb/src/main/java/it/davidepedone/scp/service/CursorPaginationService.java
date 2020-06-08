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
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * @author Davide Pedone
 * @since 1.0
 */
@Slf4j
public abstract class CursorPaginationService<T, V extends CursorPaginationSearchFilter> {

	private final MongoOperations mongoOperations;

	private final List<String> sortableFields;

	private Duration queryDurationMaxTime;

	private final String encryptionKey;

	private final ClassTypeInformation<T> classTypeInformation;

	private final Class<T> tClass;

	private final MongoPersistentEntity<?> persistentEntity;

	private static final String CIPHER_ALG = "Blowfish";

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

	public void setQueryDurationMaxTime(Duration queryDurationMaxTime) {
		this.queryDurationMaxTime = queryDurationMaxTime;
	}

	public CursorPaginationSlice<T> executeQuery(V filter) throws CursorPaginationException {

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
			String decoded = decrypt(filter.getContinuationToken());
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
				Object paramValue = getParamValue(paramValueAsString, typeInformation);

				Criteria criteria = new Criteria();
				if (Sort.Direction.DESC.equals(filter.getDirection())) {
					criteria.orOperator(where(paramName).is(paramValue).and("_id").lt(new ObjectId(id)),
							where(paramName).lt(paramValue));
				}
				else {
					criteria.orOperator(where(paramName).is(paramValue).and("_id").gt(new ObjectId(id)),
							where(paramName).gt(paramValue));
				}
				query.addCriteria(criteria);
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
			log.debug("Plain continuationToken: {}", plainToken);
			continuationToken = encrypt(plainToken);
		}

		return new CursorPaginationSlice<>(toReturn, filter.getSize(), continuationToken);
	}

	protected Object getParamValue(String paramValueAsString, TypeInformation<?> typeInformation)
			throws CursorPaginationException {
		try {
			Class<?> type = Objects.requireNonNull(typeInformation).getType();
			Object paramValue = type.isAssignableFrom(Date.class) ? new Date(Long.parseLong(paramValueAsString))
					: mongoOperations.getConverter().convertToMongoType(paramValueAsString, typeInformation);
			return Objects.requireNonNull(paramValue);
		}
		catch (Exception e) {
			throw new CursorPaginationException("Error getting parameter value: " + e.getMessage(), e);
		}
	}

	protected String getHash(V filter) {
		return DigestUtils
				.md5DigestAsHex((filter.toString() + filter.getSort() + filter.getDirection().name()).getBytes());
	}

	/**
	 * This method takes a {@MongoPersistentProperty} and an entity to retrieve the value
	 * necessary to build the continuation token using reflection. If the property is
	 * instance of a {@java.util.Date} returns the timestamp representing the value. Null
	 * is not an option because can produce unexpected pagination results in some edge
	 * cases.
	 * @param prop
	 * @param entity
	 * @return
	 * @throws CursorPaginationException if reflection fails or if value to be returned is
	 * null
	 */
	protected Object getValue(@Nullable MongoPersistentProperty prop, T entity) throws CursorPaginationException {
		if (prop == null) {
			throw new CursorPaginationException("PersistentProperty is null");
		}
		Method getter = prop.getGetter();
		if (getter == null) {
			throw new CursorPaginationException("No getter found for property " + prop.getFieldName());
		}
		Object object;
		try {
			object = getter.invoke(entity);
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			throw new CursorPaginationException(
					"Error invoking getter " + getter.getName() + " for property " + prop.getFieldName());
		}
		if (object == null) {
			throw new CursorPaginationException("Null value not allowed for property " + prop.getFieldName());
		}
		return (object instanceof Date) ? ((Date) object).getTime() : object;
	}

	protected String encrypt(String strToEncrypt) throws CursorPaginationException {
		try {
			SecretKeySpec skeyspec = new SecretKeySpec(encryptionKey.getBytes(), CIPHER_ALG);
			Cipher cipher = Cipher.getInstance(CIPHER_ALG);
			cipher.init(Cipher.ENCRYPT_MODE, skeyspec);
			byte[] encrypted = cipher.doFinal(strToEncrypt.getBytes());
			return Base64Utils.encodeToUrlSafeString(encrypted);
		}
		catch (Exception e) {
			log.error("Error encrypting continuationToken: {}", e.getMessage());
			throw new CursorPaginationException("Error encrypting token", e);
		}
	}

	protected String decrypt(String strToDecrypt) throws CursorPaginationException {
		try {
			SecretKeySpec skeyspec = new SecretKeySpec(encryptionKey.getBytes(), CIPHER_ALG);
			Cipher cipher = Cipher.getInstance(CIPHER_ALG);
			cipher.init(Cipher.DECRYPT_MODE, skeyspec);
			byte[] decrypted = cipher.doFinal(Base64Utils.decodeFromUrlSafeString(strToDecrypt));
			return new String(decrypted);
		}
		catch (Exception e) {
			log.error("Error decrypting continuationToken: {}", e.getMessage());
			throw new CursorPaginationException("Error decrypting token", e);
		}
	}

	public abstract void configSearchQuery(Query query, V filter);

}
