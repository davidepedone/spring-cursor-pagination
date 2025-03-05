Spring Cursor Pagination
==========

[![Maven Status](https://maven-badges.herokuapp.com/maven-central/it.davidepedone/spring-cursor-pagination/badge.svg?style=flat)](http://mvnrepository.com/artifact/it.davidepedone/spring-cursor-pagination)
![example workflow](https://github.com/davidepedone/spring-cursor-pagination/actions/workflows/maven.yml/badge.svg)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=davidepedone_spring-cursor-pagination&metric=alert_status)](https://sonarcloud.io/dashboard?id=davidepedone_spring-cursor-pagination)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=davidepedone_spring-cursor-pagination&metric=coverage)](https://sonarcloud.io/dashboard?id=davidepedone_spring-cursor-pagination)
[![Known Vulnerabilities](https://snyk.io/test/github/davidepedone/spring-cursor-pagination/badge.svg?targetFile=pom.xml)](https://snyk.io/test/github/davidepedone/spring-cursor-pagination?targetFile=pom.xml)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

# **Introduction**

The spring-cursor-pagination Java library allows to easily implement cursor pagination for Spring Boot projects.

This is a community-based project, not maintained by the Spring Framework Contributors (Pivotal)

# **Getting Started**
Spring-cursor-pagination is currently available only for mongodb. **NEVER** sort by fields that may contain null values to prevent unexpected results.

* First add the library to the list of your project dependencies.

```xml
   <dependency>
      <groupId>it.davidepedone</groupId>
      <artifactId>spring-cursor-pagination-mongodb</artifactId>
      <version>last-release-version</version>
   </dependency>
```

* Optionally define a POJO class to map request filter

```java
public class PostSearchFilter {
   private String author;
   // getter/setter
}
```

* Define a service class that extends `CursorPaginationService` in order to indicate entity class, a list of sortable field and customize query against database (filter and authz checks) 

```java
@Service
public class PostPaginationService extends CursorPaginationService<Post, PostSearchFilter> {

	public PostPaginationService(MongoOperations mongoOperations) {
		super(mongoOperations, List.of("createdAt", "author"), Post.class);
	}

	@Override
	public void configSearchQuery(Query query, @Nullable PostSearchFilter filter, @Nullable Principal principal) {
		Optional.ofNullable(filter).map(PostSearchFilter::getAuthor).ifPresent(a -> {
			query.addCriteria(where("author").is(a));
		});
	}

}
```

* Update or create controller method and that's it
```java
public class PostController {
    
    @Autowired
    private PostPaginationService postPaginationService;

	@GetMapping
	public CursorPaginationSlice<Post> getAll(PostSearchFilter postSearchFilter, CursorPageable pageRequest) {
		return postPaginationService.executeQuery(pageRequest, postSearchFilter, null);
	}
}
```

# **Customization**
To change query string parameter names just define a `CursorPageableHandlerMethodArgumentResolverCustomizer` bean:

```java
	@Bean
	public CursorPageableHandlerMethodArgumentResolverCustomizer customizer() {
		return c -> {
			c.setContinuationTokenParameterName("ct");
			c.setSizeParameterName("sz");
			c.setMaxPageSize(20);
		};
	}
```

# **Demo Project**
Check out a simple demo project [here](https://github.com/davidepedone/spring-cursor-pagination-demo)

# **Maven Central**

The `spring-cursor-pagination` libraries are hosted on maven central repository. 
The artifacts can be accessed at the following locations:

Releases:
* [https://oss.sonatype.org/content/groups/public/it/davidepedone/](https://oss.sonatype.org/content/groups/public/it/davidepedone/).

Snapshots:
* [https://central.sonatype.org/content/repositories/snapshots/it/davidepedone/](https://central.sonatype.org/content/repositories/snapshots/it/davidepedone/).