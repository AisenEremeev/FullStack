# Full Stack Development

Лабораторные работы Aisen Eremeev. Каждая работа хранится отдельно, чтобы её можно было открыть, запустить и сдать независимо.

| Работа | Тема | Порт | Инструкция |
|---|---|---|---|
| 2 | JPA: создание БД и доступ к данным | 8080 | [lab2](lab2/README.md) |
| 3 | RESTful web service with Spring Boot | 8083 | [lab3](lab3/README.md) |
| 4 | Spring Security and JWT | 8084 | [lab4](lab4/README.md) |
| 5 | Backend unit and integration tests | 8085 | [lab5](lab5/README.md) |
| 6 | Frontend: Node.js, npm, Vite | 5173 / 4173 | [lab6](lab6/README.md) |

Лабораторная № 1 в исходной папке отсутствовала. Backend: Java 17+, Spring Boot, Spring Data JPA, H2; frontend: Node.js 24 LTS, npm, Vite, Vanilla JavaScript.

## После клонирования

```sh
git clone https://github.com/AisenEremeev/FullStack.git
cd FullStack
```

Для backend установить JDK 21 и Maven 3.6.3+. Для frontend установить Node.js 24 LTS с npm.

```sh
# Все проверки лабораторной № 5 и отчёт покрытия
cd lab5
mvn clean verify
# Запуск backend № 5 при необходимости
mvn spring-boot:run
```

В отдельном терминале из корня репозитория:

```sh
cd lab6
npm ci
npm run dev
```

Frontend доступен на http://127.0.0.1:5173. Для кнопки загрузки клиентов запустить backend № 3 (`mvn -f lab3/pom.xml spring-boot:run` из корня); frontend не требует backend для демонстрации инструментов и счётчика.

## Запуск на исходном Windows-компьютере

Java и Maven находятся в `lab2/.tools`, Node.js и npm — в корневой `.tools`. Эти инструменты не включены в Git. PowerShell-скрипты находят их автоматически.

```powershell
powershell -ExecutionPolicy Bypass -File .\lab5\run.ps1 test
powershell -ExecutionPolicy Bypass -File .\lab6\run.ps1 dev
```

Для других backend-работ: `lab2/run.ps1`, `lab3/run.ps1`, `lab4/run.ps1`; параметр `test` запускает тесты. Остановка серверов — Ctrl+C. Данные H2 сохраняются в папке `data` соответствующей работы. Тесты используют отдельные БД в памяти.

## IntelliJ IDEA

Открывать **pom.xml нужной лабораторной как Maven-проект**, выбрать JDK 21, дождаться импорта. Backend запускается через `JpaLabApplication.main`. Работу № 6 открыть как папку и использовать npm scripts. Подробные примеры Postman и сценарии защиты находятся в README каждой работы.

## Автоматическая проверка

GitHub Actions проверяет backend № 2–5 командой `mvn clean verify`, frontend — `npm ci`, `npm run check`, `npm run build`. Отчёты тестов, JaCoCo и frontend-сборка сохраняются как artifacts workflow. Наличие конфигурации CI не означает, что удалённый запуск уже прошёл: результат смотреть во вкладке Actions.

В репозиторий входят исходники, тесты, конфигурация и инструкции. Локальные JDK/Maven/Node.js, БД, токены, настройки IDE, архивы, node_modules и результаты сборки исключены через `.gitignore`.
