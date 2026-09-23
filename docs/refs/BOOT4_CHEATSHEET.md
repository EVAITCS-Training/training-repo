# Spring Boot 4.1.x Migration Cheat Sheet (vs Boot 3.x)

> Reference for this repo. Sources: official Boot 4.0 Migration Guide, Boot 4.1.1 docs
> (testing, slices, json, spring-security, servlet), Security 7.0 migration guide, Jackson 3.0 wiki.
> Baseline: Java 17+ (we use 21), Jakarta EE 11 / Servlet 6.1, Spring Framework 7.

## A. Testing

**Starter model changed**: every technology has a `-test` companion starter; all transitively
include `spring-boot-starter-test` (don't declare it separately). Module naming:
`spring-boot-<tech>-test`, package `org.springframework.boot.<tech>.test`.

| What | Import (new unless noted) | Comes from |
|---|---|---|
| `@SpringBootTest` | `org.springframework.boot.test.context.SpringBootTest` (**unchanged**) | `spring-boot-starter-test` |
| `@WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` | `spring-boot-starter-webmvc-test` |
| `@AutoConfigureMockMvc` | `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` | same |
| `@DataJpaTest` | `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` | `spring-boot-starter-data-jpa-test` |
| `@JdbcTest` | `org.springframework.boot.jdbc.test.autoconfigure.JdbcTest` | `spring-boot-starter-jdbc-test` |
| `@JsonTest` | `org.springframework.boot.test.autoconfigure.json.JsonTest` (**unchanged**) | `spring-boot-starter-test` |
| `MockMvc` | `org.springframework.test.web.servlet.MockMvc` (unchanged) | `spring-boot-starter-webmvc-test` |
| `MockMvcTester` | `org.springframework.test.web.servlet.assertj.MockMvcTester` (AssertJ flavor) | same |
| `TestRestTemplate` | `org.springframework.boot.resttestclient.TestRestTemplate` + `@AutoConfigureTestRestTemplate` | `spring-boot-resttestclient` (+ `spring-boot-restclient` runtime) |
| `RestTestClient` (new, recommended) | `org.springframework.test.web.servlet.client.RestTestClient` + `org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient` | `spring-boot-resttestclient` |
| `WebTestClient` | `org.springframework.test.web.reactive.server.WebTestClient` + `org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient` | `spring-boot-starter-webclient-test`/webflux-test |
| `@WithMockUser`, `SecurityMockMvcRequestPostProcessors` | `org.springframework.security.test.*` (unchanged) | **`spring-boot-starter-security-test`** (required now) |
| `@MockitoBean` / `@MockitoSpyBean` | `org.springframework.test.context.bean.override.mockito.*` | spring-test; **`@MockBean`/`@SpyBean` REMOVED** |

Breaking behavior:
- `@SpringBootTest` **no longer implies MockMvc/TestRestTemplate/WebClient** — add
  `@AutoConfigureMockMvc` / `@AutoConfigureTestRestTemplate` explicitly.
- JUnit: Boot 4 manages **JUnit 6** (packages unchanged: `org.junit.jupiter.api.Test`);
  JUnit 4/vintage gone. Plain Jupiter + Mockito usage unchanged; `spring-boot-starter-test`
  still ships JUnit, AssertJ, Hamcrest, Mockito, JSONassert, JsonPath, Awaitility.
- `MockitoTestExecutionListener` removed → use `@ExtendWith(MockitoExtension.class)` for `@Mock`/`@Captor`.

Controller slice test (recommended):

```java
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest; // MockMvc auto-provided in slices
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired MockMvc mvc;              // or MockMvcTester (AssertJ)
    @MockitoBean UserService service;
    @Test void get() throws Exception {
        mvc.perform(get("/users/1")).andExpect(status().isOk());
    }
}
```

Full integration test:

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient   // or @AutoConfigureTestRestClient/@AutoConfigureMockMvc for MOCK env
class AppIT { @Autowired RestTestClient client; }
```

## B. Spring Security 7 (servlet)

Starters: `spring-boot-starter-security`; resource server:
**`spring-boot-starter-security-oauth2-resource-server`** (old `spring-boot-starter-oauth2-resource-server`
deprecated); test: `spring-boot-starter-security-test`.

Lambda DSL is **mandatory** (non-lambda chaining + `.and()` removed; `.apply()` → `.with()`).
Unchanged from idiomatic 6.x: `requestMatchers`, `csrf(AbstractHttpConfigurer::disable)`,
`sessionCreationPolicy`, `addFilterBefore`, `exceptionHandling`, `oauth2ResourceServer(rs -> rs.jwt(...))`,
`@EnableMethodSecurity`, `PasswordEncoder`/`BCryptPasswordEncoder`, `AuthenticationManager` beans.

```java
@Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a.requestMatchers("/api/auth/**").permitAll().anyRequest().authenticated())
        .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint()))
        .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

Removed/changed in 7.0:
- `AntPathRequestMatcher`/`MvcRequestMatcher` **removed**; `requestMatchers(String...)` now builds
  `PathPatternRequestMatcher` — patterns must be absolute below the context root.
- `requiresChannel` → `http.redirectToHttps(...)`; `PortResolver` removed;
  `CookieCsrfTokenRepository.setCookie*` → `setCookieCustomizer`.
- Legacy Access API (`AccessDecisionManager`/`AccessDecisionVoter`) moved to opt-in `spring-security-access`.
- OAuth2 RS: `NimbusJwtDecoder` — `typ` validation now via `JwtTypeValidator` (included in
  `JwtValidators.createDefault*`); `BearerTokenAuthenticationFilter#setBearerTokenResolver` deprecated →
  pass `BearerTokenAuthenticationConverter` to constructor.
- Jackson: `SecurityJackson2Modules` → `SecurityJacksonModules` (Jackson 3 `JsonMapper.Builder`).

Boot class relocations: `SecurityProperties`/`SecurityAutoConfiguration` →
`org.springframework.boot.security.autoconfigure`; `PathRequest` →
`org.springframework.boot.security.autoconfigure.web.servlet.PathRequest`; actuator `EndpointRequest` →
`org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest`.

Symmetric-secret JWT decoder:

```java
@Bean JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
}
```

Custom JWT filter pattern (`OncePerRequestFilter` + `addFilterBefore(...,
UsernamePasswordAuthenticationFilter.class)`) still works; note `UsernamePasswordAuthenticationFilter`
now takes an `AuthenticationManager` constructor arg. Method security: compile with `-parameters`
(Boot plugins do it by default).

## C. Jackson 3

- Coordinates: `com.fasterxml.jackson.*` → **`tools.jackson.*`**. **Annotations keep the old
  package**: `com.fasterxml.jackson.annotation` (`@JsonProperty`, `@JsonIgnore`, `@JsonFormat`,
  `@JsonInclude`, `@JsonCreator` all still work).
- Auto-configured bean is **`tools.jackson.databind.json.JsonMapper`** — inject `JsonMapper`
  (not `ObjectMapper`). To override, define a `JsonMapper` bean. Customizer:
  `JsonMapperBuilderCustomizer` (was `Jackson2ObjectMapperBuilderCustomizer`).
- **`JavaTimeModule` not needed** — jsr310, jdk8, parameter-names modules are built into
  `jackson-databind` 3. Records work natively.
- Exceptions are **unchecked**: `JacksonException extends RuntimeException`;
  `writeValueAsString` no longer throws checked exceptions.
- Renames for custom (de)serializers: `JsonSerializer`→`ValueSerializer`,
  `JsonDeserializer`→`ValueDeserializer`, `SerializerProvider`→`SerializationContext`,
  `TextNode`→`StringNode`. Boot: `@JsonComponent`→`@JacksonComponent`.
- Default changes that alter REST output: `FAIL_ON_UNKNOWN_PROPERTIES=false`,
  `WRITE_DATES_AS_TIMESTAMPS=false` (Instant → ISO-8601 strings), enums via `toString()`,
  `FAIL_ON_NULL_FOR_PRIMITIVES=true`, `FAIL_ON_TRAILING_TOKENS=true`, properties sorted alphabetically.
  `spring.jackson.use-jackson2-defaults=true` restores Jackson-2-like behavior.
- Boot properties: `spring.jackson.read/write.*` → `spring.jackson.json.read/write.*`.
- Escape hatch: deprecated `org.springframework.boot:spring-boot-jackson2` module (`spring.jackson2.*`).

## D. Web MVC on Framework 7

- `@RestController`, `@GetMapping`, `@RequestMapping`, `@ControllerAdvice`, `@ExceptionHandler`,
  `ResponseEntity`, `ProblemDetail`: **unchanged** (same packages).
- Starter: **`spring-boot-starter-webmvc`** (test: `spring-boot-starter-webmvc-test`).
  `spring-boot-starter-web` still exists but is deprecated. `spring.mvc.problemdetails.enabled=true`
  confirmed still the switch in 4.1.1.
- Boot error-handling classes moved: `ErrorController`/`ErrorAttributes` →
  `org.springframework.boot.webmvc.error.*`; `BasicErrorController` →
  `org.springframework.boot.webmvc.autoconfigure.error`.
- `HttpMessageConverters` bean **deprecated**; contributing bare `HttpMessageConverter` beans
  no longer supported → use `ClientHttpMessageConvertersCustomizer`/`ServerHttpMessageConvertersCustomizer`.
- CORS: no documented changes — `CorsConfigurationSource` bean + `http.cors(withDefaults())` fine.
- Undertow dropped; war-to-Tomcat needs `spring-boot-starter-tomcat-runtime`.

## E. Everything else

- **JPA**: `@EntityScan` moved → `org.springframework.boot.persistence.autoconfigure.EntityScan`;
  `spring.dao.exceptiontranslation.enabled` → `spring.persistence.exceptiontranslation.enabled`;
  `jakarta.persistence.*` and `spring.jpa.*` unchanged; `hibernate-jpamodelgen` → `hibernate-processor`.
- **Validation**: `spring-boot-starter-validation` + `@Valid`/`@Validated` unchanged.
- **Config**: `application.yml` mechanics unchanged; `spring-boot-properties-migrator` (runtime
  scope) catches renames. Nullability annotations → JSpecify (`org.jspecify.annotations.Nullable`).
- **Actuator**: `management.endpoints.web.exposure.include` unchanged; **liveness/readiness probes
  now enabled by default** (disable: `management.endpoint.health.probes.enabled=false`).
- **H2 console**: now its own module `spring-boot-h2console`; `spring.h2.console.enabled=true` unchanged.
- **DevTools**: live reload **off by default** (`spring.devtools.livereload.enabled=true`).
- **Build**: `spring-boot-starter-aop` → `spring-boot-starter-aspectj`; Spring Retry dependency
  management removed (use Framework `org.springframework.core.retry`); Flyway/Liquibase now need
  explicit `spring-boot-starter-flyway`/`-liquibase`.
- **Escape hatch**: `spring-boot-starter-classic` / `spring-boot-starter-test-classic` restore the
  old all-in-one classpath while migrating (deprecated path).
