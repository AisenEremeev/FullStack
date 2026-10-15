# Лабораторная № 6 — Setting up the environment and tools: frontend

Цель: настроить Node.js, npm и Vite, запустить frontend, проверить обновление при разработке и собрать production-версию. Выбран простой шаблон Vanilla JavaScript: для этой работы отдельный UI-фреймворк не требуется.

## Запуск на этом компьютере

Node.js 24 LTS и npm скачаны в корневую `.tools`. Скрипт находит их автоматически, системные настройки PATH менять не нужно.

```powershell
# Из C:\Projects\FullStack
powershell -ExecutionPolicy Bypass -File .\lab6\run.ps1 dev
```

Открой http://127.0.0.1:5173. Остановить сервер: Ctrl+C. Скрипт при отсутствии `node_modules` сначала выполняет `npm ci`; для первого скачивания зависимостей нужен интернет.

## Установка на другом компьютере

1. Установить [Node.js 24 LTS](https://nodejs.org/en/download), npm входит в установку.
2. Заново открыть терминал и проверить `node --version`, `npm --version`.
3. Открыть папку `lab6` в VS Code или IntelliJ IDEA.
4. Выполнить:

```sh
npm ci
npm run dev
```

Если PowerShell блокирует `npm.ps1`, используй `npm.cmd ci` и `npm.cmd run dev`. Минимум для этого проекта — Node.js 22.12; для воспроизводимости выбран Node.js 24, это также указано в `.nvmrc`.

## Команды

| Команда | Назначение |
|---|---|
| `node --version`, `npm --version` | Проверить окружение |
| `npm ci` | Установить точные версии из `package-lock.json` |
| `npm run dev` | Vite dev server на 127.0.0.1:5173 |
| `npm run check` | Проверить синтаксис JavaScript |
| `npm run build` | Собрать оптимизированные файлы в `dist/` |
| `npm run preview` | Проверить готовую сборку на 127.0.0.1:4173 |

Локальные эквиваленты без настройки PATH: `run.ps1 install`, `run.ps1 check`, `run.ps1 build`, `run.ps1 preview`. Preview запускать после build. Preview — средство локальной проверки, не production-сервер.

Обычный старт нового аналогичного проекта: `npm create vite@latest my-frontend -- --template vanilla`. В этой работе файлы уже подготовлены, повторять создание проекта не нужно.

## Что есть на странице

- Краткие карточки Node.js, npm и Vite.
- Кнопки счётчика для проверки JavaScript и DOM-событий.
- Подпись Development/Production build из `import.meta.env`.
- Загрузка клиентов из лабораторной № 3 через proxy `/api`.
- Состояния ожидания, загрузки, пустой базы и ошибки соединения.

Frontend запускается самостоятельно. Backend нужен только для кнопки загрузки клиентов. При его отсутствии выводится понятная ошибка, остальные элементы работают.

## Подключение backend

В отдельном терминале запустить лабораторную № 3:

```powershell
powershell -ExecutionPolicy Bypass -File .\lab3\run.ps1
```

Создать запись через Postman: `POST http://localhost:8083/api/customers`, Body → raw → JSON:

```json
{"firstName":"Ada","lastName":"Lovelace"}
```

Нажать «Загрузить клиентов» на frontend. Браузер отправляет `/api/customers` на Vite, а Vite пересылает запрос на `http://127.0.0.1:8083`. Так на этапе разработки не требуется настройка CORS. По умолчанию используется открытый API № 3; защищённый backend № 4/5 требует JWT, а форма входа не входит в тему этой работы.

Чтобы изменить адрес, скопировать `.env.example` в `.env`, отредактировать `BACKEND_URL` и перезапустить Vite. `VITE_APP_TITLE` меняет название в шапке. Переменные с префиксом `VITE_` включаются в клиентский код — пароли и приватные ключи в них хранить нельзя. `.env` исключён из Git, `.env.example` публикуется.

Dev proxy и preview proxy работают только пока запущен Vite. При размещении `dist` на реальном сервере для `/api` потребуется отдельный reverse proxy или другой адрес API.

## Структура

- `package.json` — npm scripts, зависимости и требования к Node.js.
- `package-lock.json` — точные версии и контрольные суммы зависимостей.
- `vite.config.js` — порты и proxy; `strictPort` не позволяет незаметно сменить занятый порт.
- `index.html` — HTML страницы.
- `src/main.js` — ES-модуль: импорт CSS, счётчик, fetch и обновление DOM.
- `src/style.css` — адаптивные стили.
- `.nvmrc` — основная версия Node.js для менеджеров версий.

Node.js исполняет инструменты сборки, npm устанавливает пакеты и запускает scripts, Vite обслуживает ES-модули во время разработки и собирает результат. Сам frontend JavaScript исполняется в браузере. `node_modules` и `dist` создаются локально и не отправляются в Git.

## Как показать преподавателю

1. Показать версии Node.js и npm. На этом компьютере без системного PATH можно выполнить `..\.tools\node-v24.22.0-win-x64\node.exe --version` и `..\.tools\node-v24.22.0-win-x64\npm.cmd --version` из папки `lab6`.
2. Открыть `package.json`, объяснить команды dev/build/preview.
3. Запустить dev server, открыть страницу, нажать «Добавить +1» и «Сбросить».
4. Изменить цвет в `src/style.css`: CSS обновится без ручной перезагрузки (HMR). Изменение JS в этом Vanilla-проекте может приводить к перезагрузке страницы и сбросу счётчика.
5. По желанию показать загрузку клиентов из № 3.
6. Выполнить build, показать `dist`, запустить preview и надпись Production build.

Источники: [Vite Getting Started](https://vite.dev/guide/), [npm ci](https://docs.npmjs.com/cli/v11/commands/npm-ci), [Node.js](https://nodejs.org/en/learn/getting-started/introduction-to-nodejs).
