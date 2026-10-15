# Лабораторная № 4

**Securing the backend: Spring Security and JWT**

Цель: защитить REST API из работы № 3. Реализованы регистрация, проверка пароля, выдача подписанного JWT и доступ к API по Bearer-токену.

## Запуск

В PowerShell из папки работы:

```powershell
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

Сервер: http://localhost:8084. Java и Maven автоматически берутся из соседней `lab2/.tools`. На другом компьютере нужны JDK 17+ и Maven. При открытии `/api/customers` в браузере без токена ожидается **401** — защита работает.

## Демонстрация

В другом окне PowerShell из этой же папки:

```powershell
powershell -ExecutionPolicy Bypass -File .\demo.ps1
```

Скрипт проверяет отказ без токена, регистрирует пользователя с уникальным именем и случайным паролем, входит, выполняет CRUD с токеном, проверяет неправильный пароль и поддельный токен. Созданный клиент удаляется; демонстрационный пользователь остаётся в учебной БД. Пароль и токен скрипт не печатает.

Ручная демонстрация (пароль ниже — только учебный пример):

```powershell
$base = 'http://localhost:8084'
$credentials = @{ username = 'student'; password = 'Lab4-example-123!' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$base/api/auth/register" -ContentType 'application/json' -Body $credentials
$auth = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json' -Body $credentials
$headers = @{ Authorization = "Bearer $($auth.accessToken)" }
Invoke-RestMethod -Uri "$base/api/auth/me" -Headers $headers
$customer = Invoke-RestMethod -Method Post -Uri "$base/api/customers" -Headers $headers -ContentType 'application/json' -Body '{"firstName":"Ada","lastName":"Lovelace"}'
Invoke-RestMethod -Uri "$base/api/customers" -Headers $headers
Invoke-RestMethod -Method Put -Uri "$base/api/customers/$($customer.id)" -Headers $headers -ContentType 'application/json' -Body '{"firstName":"Grace","lastName":"Hopper"}'
Invoke-RestMethod -Method Delete -Uri "$base/api/customers/$($customer.id)" -Headers $headers
```

При повторной регистрации `student` ожидается 409. В этом случае выполнить только вход с исходным паролем либо выбрать новое имя.

## Маршруты и статусы

| Метод и путь | Доступ | Результат |
|---|---|---|
| POST `/api/auth/register` | Публичный | 201, ID и имя пользователя |
| POST `/api/auth/login` | Публичный | 200, accessToken, tokenType, expiresIn |
| GET `/api/auth/me` | JWT | 200, имя пользователя |
| GET `/api/customers` | JWT | Список, можно передать `?lastName=Hopper` |
| GET `/api/customers/{id}` | JWT | Один клиент |
| POST `/api/customers` | JWT | Создание, 201 |
| PUT `/api/customers/{id}` | JWT | Изменение, 200 |
| DELETE `/api/customers/{id}` | JWT | Удаление, 204 |

Регистрация и вход принимают `username` и `password`. Имя: 3–40 латинских букв, цифр или `_`, хранится в нижнем регистре. Пароль: 8–64 символа и не более 72 байт UTF-8 (ограничение BCrypt). Пароли регистрозависимы. Для POST/PUT клиента обязательны `firstName` и `lastName`, максимум 100 символов.

- 400 — неверный JSON или поля.
- 401 — неверный логин/пароль, отсутствующий, повреждённый или истёкший токен.
- 403 — аутентифицированный пользователь обратился к запрещённому пути.
- 404 — клиент не найден при запросе с действующим токеном.
- 409 — имя пользователя уже занято.

Ошибки контроллеров имеют JSON Problem Details. Отказ фильтра Spring Security может иметь пустое тело; клиент должен проверять HTTP-статус и `WWW-Authenticate`.

## Как работает защита

1. Регистрация сохраняет пользователя в таблице `app_users`; пароль превращается в BCrypt-хеш с солью. Исходный пароль не записывается и не возвращается.
2. При входе `AuthenticationManager` и `DaoAuthenticationProvider` проверяют пароль через `PasswordEncoder` и `UserDetailsService`.
3. `NimbusJwtEncoder` выдаёт JWT с подписью **RS256**, именем пользователя (`sub`), издателем (`iss`), временем выдачи (`iat`) и истечения (`exp`). Срок действия — **900 секунд**.
4. Клиент отправляет `Authorization: Bearer <token>` при каждом запросе.
5. Фильтр Spring Security проверяет подпись, издателя и время действия через `JwtDecoder`, затем разрешает доступ к контроллеру.

JWT подписан, но не зашифрован: его содержимое можно прочитать. Пароль в JWT не включается. Сервер не создаёт HTTP-сессию (`STATELESS`). CSRF отключён для API, где авторизация передаётся явно в заголовке, а не автоматически в cookie; form login и HTTP Basic не настроены.

В этой лабораторной все зарегистрированные пользователи имеют одинаковый доступ к общему списку клиентов. Персональная принадлежность записей и роли администратора не входят в предоставленное условие.

Ключ RSA генерируется в памяти при старте: **после перезапуска нужно войти заново**, старые JWT перестают действовать. Пользователи и клиенты сохраняются в `data/lab4.mv.db`. В исходниках нет постоянного приватного ключа или готовых аккаунтов. H2-консоль отключена, сервер доступен только на локальном компьютере. Учебная реализация не включает refresh-токены, отзыв токенов и ограничение частоты входа; при удалении токена на клиенте уже выданный JWT действует до истечения срока или перезапуска. Для размещения в интернете нужны HTTPS и отдельная настройка хранения ключей и защиты входа.

## Файлы для защиты

- `SecurityConfig` — правила доступа, stateless-режим, BCrypt, AuthenticationManager, RSA и JWT decoder/encoder.
- `AuthController` — регистрация, вход и текущий пользователь; API возвращает DTO, не сущность с хешем.
- `AppUser`, `UserRepository` — хранение пользователей и уникальность имени на уровне БД.
- `CustomerController`, `CustomerService`, `CustomerRepository` — защищённый REST API и JPA.
- `SecurityIntegrationTest` — реальная регистрация и вход, хеширование, 401/403/409, повреждённые и истёкшие JWT.
- `CustomerIntegrationTest` — CRUD после получения настоящего JWT через login.

```powershell
.\run.ps1 test
.\run.ps1 package
```

Тесты используют отдельные H2 в памяти. Исполняемый JAR: `target/security-lab-1.0.0.jar`.

Источник: [Spring Security — JWT Resource Server](https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html).
