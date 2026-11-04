# TODO-лист: Spring Boot REST API + CI/CD

REST API для управления задачами. Задачи хранятся в памяти, без БД. GitHub Actions собирает проект, прогоняет тесты, собирает и пушит Docker-образ в Docker Hub, а затем выкатывает его на виртуальную машину по стратегии **blue-green**.

## API

| Метод | URL | Описание | Ответ |
|---|---|---|---|
| POST | `/tasks` | добавить задачу | 201, 400 при ошибке валидации |
| GET | `/tasks` | все задачи | 200 |
| GET | `/tasks/{id}` | одна задача | 200 / 404 |
| PUT | `/tasks/{id}` | обновить задачу (название, выполнена) | 200 / 400 / 404 |
| DELETE | `/tasks/{id}` | удалить задачу | 204 / 404 |

Пример задачи:

```json
{
  "id": 1,
  "title": "Сделать ДЗ по CI/CD",
  "completed": false,
  "createdAt": "2023-10-25"
}
```

Валидация: `title` обязателен, не может быть пустым и не длиннее 255 символов. `id` и `createdAt` назначает сервер.

```bash
curl -X POST localhost:8080/tasks -H 'Content-Type: application/json' -d '{"title":"Купить молоко"}'
curl localhost:8080/tasks
curl -X PUT localhost:8080/tasks/1 -H 'Content-Type: application/json' -d '{"title":"Купить молоко","completed":true}'
curl -X DELETE localhost:8080/tasks/1
```

## Локальный запуск

```bash
mvn verify                      # сборка + тесты + проверка покрытия >= 70% (JaCoCo)
java -jar target/todo.jar
# или в Docker
docker build -t todo-app . && docker run -p 8080:8080 todo-app
```

Отчёт о покрытии: `target/site/jacoco/index.html`.

## CI/CD (`.github/workflows/ci-cd.yml`)

1. **build-and-test**: JDK 17, кэш Maven-зависимостей, `mvn package`, `mvn verify` (тесты и порог покрытия 70%), JAR и отчёт о покрытии сохраняются как артефакты.
2. **docker-build**: вход в Docker Hub, многостадийная сборка, пуш образа с тегами `latest` и `<sha коммита>`.
3. **deploy**: заходит на VM по SSH и выполняет blue-green деплой, потом проверяет, что приложение отвечает снаружи.
4. **notify**: сводка результатов в Summary запуска. Если заданы секреты Telegram, дополнительно приходит сообщение в Telegram.

Для pull request выполняется только build-and-test.

### Blue-green

На VM работает nginx (контейнер `todo-proxy`) на порту 8080 и два «цвета» приложения: `todo-blue` и `todo-green`. При деплое:

1. Скачивается новый образ.
2. Новая версия запускается в неактивном цвете, старая в это время продолжает обслуживать трафик.
3. Деплой ждёт, пока сработает `HEALTHCHECK`. Если контейнер не стал healthy, новый удаляется и пайплайн падает, а старая версия продолжает работать.
4. nginx переключается на новый контейнер без простоя.
5. Старый контейнер останавливается. Он остаётся остановленным, чтобы можно было быстро откатиться.

## Настройка

### 1. Docker Hub

Создайте Access Token: **Account settings → Personal access tokens**, права Read & Write. Репозиторий `todo-app` создастся сам при первом пуше и должен быть публичным, иначе на VM понадобится `docker login`.

### 2. Виртуальная машина (Ubuntu)

```bash
# Установите необходимые пакеты
sudo apt update && sudo apt install -y docker.io openssh-server curl
sudo systemctl enable --now docker
sudo usermod -aG docker $USER        # чтобы деплой работал без sudo; перелогиньтесь

# Настройте firewall (сначала SSH, потом enable — иначе отрежете себе доступ)
sudo ufw allow <SSH_PORT>/tcp
sudo ufw allow 8080/tcp
sudo ufw enable

# Отдельный ключ для деплоя (на своём компьютере)
ssh-keygen -t ed25519 -f ~/.ssh/todo_deploy -N ""
ssh-copy-id -i ~/.ssh/todo_deploy.pub -p <SSH_PORT> <user>@<vm_ip>
```

Если VM стоит дома за роутером, пробросьте на неё порты `<SSH_PORT>` и `8080`. GitHub Actions подключается к VM из интернета, поэтому нужен внешний IP или DDNS-имя.

Проверьте с другой машины: `ssh -i ~/.ssh/todo_deploy -p <SSH_PORT> <user>@<внешний_ip>`.

### 3. Секреты GitHub

**Settings → Secrets and variables → Actions → New repository secret**:

| Секрет | Значение |
|---|---|
| `DOCKER_USERNAME` | логин Docker Hub |
| `DOCKER_PASSWORD` | Access Token Docker Hub (не пароль от аккаунта) |
| `VM_HOST` | внешний IP или домен VM |
| `VM_USER` | пользователь на VM |
| `VM_SSH_KEY` | **приватный** ключ целиком (`cat ~/.ssh/todo_deploy`) |
| `VM_SSH_PORT` | порт SSH (обычно `22`) |
| `TELEGRAM_TOKEN`, `TELEGRAM_CHAT_ID` | необязательно: уведомления в Telegram |

Секреты используются только через `${{ secrets.* }}`, в коде их нет, а в логах GitHub заменяет их на `***`.

## Структура

```
├── .github/workflows/ci-cd.yml
├── Dockerfile, .dockerignore
├── pom.xml
└── src
    ├── main/java/ru/example/todo
    │   ├── controller/TaskController.java
    │   ├── service/TaskService.java        # List<Task> в памяти
    │   ├── model/Task.java
    │   ├── dto/TaskRequest.java            # @NotBlank, @Size
    │   └── exception/...                   # 404 / 400
    └── test/java/ru/example/todo           # MockMvc + unit-тесты
```
