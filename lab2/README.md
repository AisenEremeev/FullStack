# Лабораторная работа № 2 — Using JPA to create and access a database

Самостоятельный учебный проект по Full Stack Development. Цель: создать реляционную базу данных по Java-сущности и реализовать доступ к данным через JPA. Дополнительных требований к варианту и СУБД не предоставлено.

## Технологии

Java 17+, Maven, Spring Boot 3.5.5, Spring Data JPA, Hibernate, H2. H2 работает в файловом режиме, отдельный сервер БД не нужен. В этой рабочей папке подготовлены локальные JDK и Maven в `.tools`.

## Запуск в Windows

Открыть PowerShell в папке проекта:

```powershell
.\run.ps1
```

Если политика PowerShell запрещает запуск скрипта, для отдельного процесса:

```powershell
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

Приложение доступно по адресу http://localhost:8080/api/customers. Остановить: Ctrl+C.
Первый запуск требует интернет для Maven-зависимостей. При переносе исходников на другой компьютер установить JDK 17 или новее и Maven 3.6.3 или новее, настроить `JAVA_HOME` и `PATH`. Тогда можно использовать обычные команды `mvn test` и `mvn spring-boot:run`.

```powershell
.\run.ps1 test
.\run.ps1 package
```

Сборка создаёт `target/jpa-lab-1.0.0.jar`; при доступном Java запуск: `java -jar target/jpa-lab-1.0.0.jar`.

## Как проверить работу

В другом окне PowerShell выполнить:

```powershell
$base = 'http://localhost:8080/api/customers'
$customer = Invoke-RestMethod -Method Post -Uri $base -ContentType 'application/json' -Body '{"firstName":"Ada","lastName":"Lovelace"}'
$customer
Invoke-RestMethod -Uri $base
Invoke-RestMethod -Uri "$base/$($customer.id)"
Invoke-RestMethod -Uri "$base`?lastName=Lovelace"
Invoke-RestMethod -Method Put -Uri "$base/$($customer.id)" -ContentType 'application/json' -Body '{"firstName":"Grace","lastName":"Hopper"}'
```

Остановить приложение и запустить повторно из той же папки. Запрос `Invoke-RestMethod -Uri $base` должен вернуть сохранённую Grace Hopper. Затем удалить запись:

```powershell
Invoke-RestMethod -Method Delete -Uri "$base/$($customer.id)"
Invoke-RestMethod -Uri $base
```

После удаления список пуст, если других записей не было. Идентификатор генерирует БД, он не обязан начинаться с 1 после удаления данных.

## API

| Метод | URL | Результат |
|---|---|---|
| POST | `/api/customers` | Создание, HTTP 201 и заголовок Location |
| GET | `/api/customers` | Список, HTTP 200 |
| GET | `/api/customers/{id}` | Запись по ID, HTTP 200 |
| GET | `/api/customers?lastName=Hopper` | Поиск по фамилии без учёта регистра |
| PUT | `/api/customers/{id}` | Изменение имени и фамилии, HTTP 200 |
| DELETE | `/api/customers/{id}` | Удаление, HTTP 204 |

POST и PUT принимают JSON с `firstName` и `lastName`. Оба поля обязательны, не могут быть пустыми или состоять из пробелов; максимум 100 символов. Некорректный ввод возвращает HTTP 400, отсутствующий ID — HTTP 404. Приложение начинает работу с пустой базой, демонстрационные данные создаются командами выше.

## Просмотр базы данных

Открыть http://localhost:8080/h2-console во время работы приложения:

- Driver Class: `org.h2.Driver`
- JDBC URL: `jdbc:h2:file:./data/lab2`
- User Name: `sa`
- Password: оставить пустым

```sql
SELECT * FROM CUSTOMERS ORDER BY ID;
```

Файл БД: `data/lab2.mv.db` относительно каталога запуска. Hibernate создаёт таблицу при первом запуске благодаря `spring.jpa.hibernate.ddl-auto=update`. Этот режим удобен для лабораторной; схема не удаляется при остановке. `WRITE_DELAY=0` отключает отложенную запись H2, чтобы подтверждённые изменения сразу записывались на диск. В консоли приложения видны SQL-запросы. H2-консоль и настройки без пароля предназначены для локального учебного запуска; сервер привязан к 127.0.0.1.

## Структура и объяснение для защиты

- `Customer` — JPA-сущность. `@Entity` связывает Java-класс с таблицей, `@Table` задаёт имя `customers`, `@Id` обозначает первичный ключ, `@GeneratedValue` поручает генерацию ID базе. `@Column` задаёт ограничения столбцов. Конструктор без аргументов нужен JPA.
- `CustomerRepository` наследует `JpaRepository<Customer, Long>`. Spring Data создаёт реализацию интерфейса, предоставляя `save`, `findAll`, `findById` и `delete`. Метод `findByLastNameIgnoreCaseOrderByIdAsc` преобразуется в запрос по имени метода.
- `CustomerController` принимает HTTP-запросы и обращается к репозиторию. При изменении используется `@Transactional`: Hibernate обнаруживает изменения управляемой сущности и выполняет UPDATE при завершении транзакции (dirty checking).
- `CustomerRequest` отделяет входные данные от сущности и проверяет обязательность и длину полей.
- `application.properties` задаёт подключение к H2 и создание схемы.
- `CustomerIntegrationTest` проверяет полный CRUD-цикл через HTTP-слой с настоящими Spring Data JPA и H2, поиск, ошибки 400/404 и состояние БД. Тесты используют отдельную базу в памяти, поэтому не изменяют учебные данные на диске.

Путь запроса: HTTP → Controller → Repository → Hibernate (реализация JPA) → H2. JPA — спецификация отображения объектов на реляционные таблицы; Hibernate — её реализация; Spring Data JPA упрощает написание репозиториев. Таблица содержит `id BIGINT` (первичный ключ), `first_name VARCHAR(100) NOT NULL`, `last_name VARCHAR(100) NOT NULL`.

Для демонстрации преподавателю: запустить приложение, создать запись, показать её в API и H2-консоли, выполнить поиск и изменение, перезапустить приложение для проверки сохранности, удалить запись и запустить тесты.

Основа: [официальное руководство Spring — Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/). Проект расширяет учебный пример CRUD API, валидацией и файловым хранением.
