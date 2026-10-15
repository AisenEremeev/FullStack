# Лабораторная № 5 — Testing the backend: unit and integration tests

Цель: проверить backend на нескольких уровнях и показать отличие unit-тестов от интеграционных. Самостоятельный проект продолжает лабораторную № 4: Spring Boot, JPA, H2, регистрация и JWT. Код backend сохранён, основная новая работа — тестирование и конфигурация сборки.

## Быстрый запуск на этом компьютере

Из `C:\Projects\FullStack`:

```powershell
powershell -ExecutionPolicy Bypass -File .\lab5\run.ps1 test
```

Скрипт использует Java/Maven из `lab2/.tools`, выполняет `mvn verify` и создаёт HTML-отчёт `lab5/target/site/jacoco/index.html`. Открой этот файл в браузере. Приложение отдельно запускать не нужно: тесты сами создают необходимые контексты и сервер.

## Команды Maven

На другом компьютере нужны JDK 17+ и Maven 3.6.3+. Открыть `lab5/pom.xml` в IntelliJ IDEA как Maven-проект; выбрать JDK 21. Команды выполнять из `lab5`:

| Команда | Что выполняется |
|---|---|
| `mvn test` | Только 8 unit-тестов через Surefire (`*Test`) |
| `mvn verify` | 8 unit + 13 интеграционных тестов, JAR и JaCoCo |
| `mvn clean verify` | Полная проверка с очисткой старых результатов |
| `mvn verify -Dtest=NoUnitTests -Dsurefire.failIfNoSpecifiedTests=false` | Только 13 интеграционных тестов через Failsafe (`*IT`) |
| `mvn spring-boot:run` | Запуск backend на порту 8085 |

Windows-эквиваленты: `run.ps1 unit`, `run.ps1 test`, `run.ps1 integration`, `run.ps1 run`. Команда `run.ps1 package` тоже вызывает `verify`, чтобы интеграционные тесты не пропускались при сборке.

## Что проверяется

| Класс | Тип | Количество | Проверки |
|---|---|---:|---|
| `CustomerServiceTest` | Unit: JUnit 5 + Mockito | 8 | Список, поиск, ID, 404, нормализация ввода, изменение и удаление |
| `CustomerRepositoryIT` | JPA slice: `@DataJpaTest` | 3 | Сохранение/чтение, поиск без учёта регистра, обновление/удаление |
| `CustomerApiIT` | Полный Spring context + MockMvc | 3 | CRUD с настоящим JWT, валидация, неизвестный ID |
| `SecurityApiIT` | Полный Spring context + MockMvc | 6 | BCrypt, вход, отсутствие/подделка/истечение JWT, issuer, 403 и дубли имён |
| `BackendHttpIT` | Полный Spring context + настоящий HTTP | 1 | Регистрация → вход → CRUD через embedded Tomcat на случайном порту |

MockMvc проверяет MVC и security filters без сетевого соединения. `BackendHttpIT` использует `TestRestTemplate` и реальное соединение к серверу. Это разные уровни интеграционного тестирования.

Unit-тесты запускаются без Spring и БД. `@Mock` создаёт замену репозитория, `@InjectMocks` передаёт её сервису. `when(...).thenReturn(...)` задаёт поведение зависимости; `assertThat` проверяет результат; `verify` — взаимодействие. В каждом тесте применяется Arrange → Act → Assert.

JPA-тесты работают с настоящей H2. После `flush` и `clear` данные читаются заново из базы, а не из persistence context. `@DataJpaTest` откатывает транзакцию после теста. API-тесты очищают свои тестовые таблицы между сценариями. Все интеграционные БД находятся в памяти; файл `data/lab5.mv.db` не используется и не изменяется тестами. H2 не проверяет особенности диалекта PostgreSQL/MySQL — эти СУБД в условии не требуются.

## Отчёты

- `target/surefire-reports` — результаты unit-тестов.
- `target/failsafe-reports` — результаты интеграционных тестов.
- `target/site/jacoco/index.html` — покрытие backend тестами, включая строки и ветвления.

JaCoCo добавляет агент при запуске тестов и объединяет измерения в `target/jacoco.exec`. При `verify` строится отчёт. Покрытие помогает найти непроверенные ветви, но само по себе не доказывает корректность; например, ветка конкурентной регистрации требует отдельного сценария. При сравнении покрытия используй `clean verify`, чтобы не учитывать старые запуски.

## Как показать преподавателю

1. Открыть `CustomerServiceTest` и объяснить, почему репозиторий заменён mock-объектом.
2. Запустить `mvn test`: показать 8 успешных unit-тестов.
3. Открыть `CustomerRepositoryIT`, показать `flush()` и `clear()`.
4. Запустить `mvn verify`: показать 13 интеграционных тестов и `BUILD SUCCESS`.
5. Открыть отчёт JaCoCo и любую строку покрытия `CustomerService`.
6. Показать `BackendHttpIT`: тест сам запускает Tomcat на случайном порту и выполняет запросы с JWT.

При желании запустить сервер и `demo.ps1`: его сценарий работает на порту 8085. API совпадает с лабораторной № 4, но использует отдельную БД. JWT действует 15 минут; после перезапуска нужно снова войти.

Источники: [Spring Boot testing](https://docs.spring.io/spring-boot/3.5/reference/testing/index.html), [Maven Failsafe](https://maven.apache.org/surefire/maven-failsafe-plugin/), [JaCoCo Maven](https://www.jacoco.org/jacoco/trunk/doc/maven.html).
