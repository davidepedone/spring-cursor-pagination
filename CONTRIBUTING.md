# How to contribute

## Code Conventions and Housekeeping
None of these is essential for a pull request, but they will all help.  They can also be
added after the original pull request but before a merge.

* We use the [Spring JavaFormat](https://github.com/spring-io/spring-javaformat) project
  to apply code formatting conventions (validated by spring-javaformat-maven-plugin). You can also install the [Spring JavaFormat IntelliJ Plugin](https://github.com/spring-io/spring-javaformat/#intellij-idea).
* Add some Javadocs.
* A few unit tests would help a lot as well -- someone has to do it.
* When writing a commit message please follow [these conventions](#commit-message-format)

## Commit Message Format

Each commit message consists of a **header** and an optional **body**,
separated by an empty line.

#### Header

Format: `[type][jira-key]: subject`.

**type** must have one of the following values:

* **feat**: a new feature
* **fix**: a bug fix
* **docs**: documentation only changes
* **style**: changes that do not affect the meaning of the code (white-space,
  formatting, missing semi-colons, etc)
* **refactor**: a code change that neither fixes a bug nor adds a feature
* **perf**: a code change that improves performance
* **test**: adding missing tests
* **chore**: changes to the build process or auxiliary tools and libraries such
  as documentation generation

**jira-key** is optional.

**subject** is a succinct description of the change and:

* uses the imperative, present tense: "change" not "changed" nor "changes"
* doesn't capitalize the first letter
* has no dot (.) at the end

#### Body

Just as in the **subject**, use the imperative, present tense: "change" not
"changed" nor "changes". The body should include the motivation for the change
and contrast this with previous behavior.

#### Example

```
[chore][DOC-1]: dev environment setup

Add linter, add base dependencies.
```