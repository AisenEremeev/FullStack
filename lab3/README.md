# Лабораторная № 3

**Creating a RESTful web service with Spring Boot**

Цель: создать HTTP API, которое принимает и возвращает JSON и предоставляет CRUD-операции над клиентами. Проект развивает работу № 2: HTTP-слой отделён от бизнес-логики, обработка ошибок возвращает структурированные ответы.

## Запуск

В PowerShell из папки этой работы:

```powershell
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

Открыть http://localhost:8083/api/customers. Начальный ответ — `[]`. Java и Maven берутся из соседней папки `lab2/.tools`; для переноса на другой компьютер нужны JDK 17+ и Maven. Остановка: Ctrl+C.

## REST API

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| GET | `/api/customers` | Список клиентов | 200 |
| GET | `/api/customers/{id}` | Клиент по ID | 200 |
| GET | `/api/customers?lastName=Lovelace` | Поиск без учёта регистра | 200 |
| POST | `/api/customers` | Создание | 201 + Location |
| PUT | `/api/customers/{id}` | Полная замена имени и фамилии | 200 |
| DELETE | `/api/customers/{id}` | Удаление | 204, пустое тело |

Тело POST и PUT, заголовок `Content-Type: application/json`:

```json
{"firstName":"Ada","lastName":"Lovelace"}
```

Ответ при создании, ID приведён для примера:

```json
{"id":1,"firstName":"Ada","lastName":"Lovelace"}
```

Оба поля обязательны, максимум 100 символов, строки из пробелов запрещены. Пробелы по краям удаляются. Некорректные данные, JSON или ID возвращают 400; отсутствующая запись — 404; неподдерживаемый HTTP-метод — 405. Ошибки представлены в формате `application/problem+json`, при валидации дополнительно есть объект `errors` с сообщениями по полям.

## Демонстрация

Пока сервер работает, открыть второе окно PowerShell в папке работы:

```powershell
powershell -ExecutionPolicy Bypass -File .\demo.ps1
```

Скрипт создаёт одного клиента, читает, ищет и изменяет его, затем удаляет только созданную им запись. Также проверяет ошибки 400 и 404.

Ручной вариант:

```powershell
$base = 'http://localhost:8083/api/customers'
$customer = Invoke-RestMethod -Method Post -Uri $base -ContentType 'application/json' -Body '{"firstName":"Ada","lastName":"Lovelace"}'
Invoke-RestMethod -Uri "$base/$($customer.id)"
Invoke-RestMethod -Uri "$base`?lastName=Lovelace"
Invoke-RestMethod -Method Put -Uri "$base/$($customer.id)" -ContentType 'application/json' -Body '{"firstName":"Grace","lastName":"Hopper"}'
Invoke-RestMethod -Method Delete -Uri "$base/$($customer.id)"
```

## База данных и устройство

Данные хранятся в `data/lab3.mv.db` и сохраняются между запусками. H2-консоль: http://localhost:8083/h2-console; JDBC URL `jdbc:h2:file:./data/lab3`, пользователь `sa`, пароль пустой. SQL для просмотра: `SELECT * FROM CUSTOMERS;`.

Путь запроса: `CustomerController → CustomerService → CustomerRepository → Hibernate → H2`.

- `@RestController` и `@RequestMapping` объявляют REST-контроллер и общий путь.
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` сопоставляют методы HTTP обработчикам.
- `@RequestBody` преобразует JSON в DTO `CustomerRequest`, `@Valid` запускает проверку.
- `ResponseEntity` управляет статусом, заголовками и телом ответа.
- `CustomerService` содержит операции с данными и границы транзакций. При PUT Hibernate сохраняет изменённую сущность при завершении транзакции.
- `ApiExceptionHandler` формирует ошибки, не требуя обработки в каждом методе контроллера.

REST представляет данные как ресурсы с адресами, использует стандартные HTTP-методы и коды ответа. GET читает ресурс; POST создаёт; PUT заменяет его поля; DELETE удаляет. Повторный PUT с теми же данными не меняет конечный результат. Повторное удаление возвращает 404, но запись остаётся удалённой.

## Тестирование и сборка

```powershell
.\run.ps1 test
.\run.ps1 package
```

Четыре интеграционных теста проверяют CRUD, поиск, сохранение изменений в БД, валидацию, отсутствующий ресурс, неверный JSON и структуру ошибок. Используется отдельная H2 в памяти. Исполняемый JAR: `target/rest-lab-1.0.0.jar`.

Источник: [Spring — Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/).
