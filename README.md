# Documents Service

Backend-сервис для управления документами с поддержкой статусов, истории изменений и реестра утверждений.

## Технологический стек

- **Java 17**
- **Spring Boot 4.0.2**
- **PostgreSQL 16** (Docker Compose)
- **JPA/Hibernate**
- **Liquibase** (миграции БД)
- **Maven**

## Функциональность

### Документы
- Создание документов со статусом DRAFT
- Автоматическая генерация уникального номера документа
- Перевод документов по статусам: DRAFT → SUBMITTED → APPROVED
- История изменений статусов с указанием инициатора и комментария

### API Endpoints

#### 1. Создать документ
```
POST /api/documents
Headers: X-Initiator: <initiator>
Body: {
  "author": "Author Name",
  "title": "Document Title",
  "content": "Document Content"
}
```

#### 2. Получить документ
```
GET /api/documents/{id}?withHistory=true
```

#### 3. Получить документы по списку ID
```
POST /api/documents/batch
Body: [1, 2, 3]
```

#### 4. Отправить на согласование
```
POST /api/documents/submit
Body: {
  "ids": [1, 2, 3],
  "initiator": "user123",
  "comment": "Optional comment"
}
```
Возвращает список `BatchResult` для каждого ID с результатом операции.

#### 5. Утвердить документы
```
POST /api/documents/approve
Body: {
  "ids": [1, 2, 3],
  "initiator": "approver123",
  "comment": "Optional comment"
}
```
При успешном утверждении создается запись в реестре утверждений. При ошибке записи в реестр - статус документа откатывается.

#### 6. Поиск документов
```
GET /api/documents/search?status=DRAFT&author=Author Name&dateFrom=2024-01-01T00:00:00Z&dateTo=2024-12-31T23:59:59Z&page=0&size=20&sortBy=createdAt&sortDir=DESC
```
Фильтры:
- `status` - статус документа (DRAFT, SUBMITTED, APPROVED)
- `author` - автор документа
- `dateFrom` / `dateTo` - период по дате создания (ISO 8601 формат)

Поддерживается пагинация и сортировка.

#### 7. Тест конкурентного утверждения
```
POST /api/documents/concurrent-approve-test?documentId=1&threads=5&attempts=10
Headers: X-Initiator: <initiator>
```
Запускает параллельные попытки утверждения одного документа и возвращает статистику.

## Запуск сервиса

### Требования
- Java 17+
- Docker и Docker Compose
- Maven 3.6+

### Шаги запуска

1. **Запустить PostgreSQL через Docker Compose:**
```bash
docker-compose up -d
```

2. **Дождаться полной инициализации PostgreSQL** (обычно 5-10 секунд)

3. **Проверить, что PostgreSQL доступен:**
```bash
docker-compose ps
```

4. **Собрать проект:**
```bash
mvn clean install
```

5. **Запустить приложение:**
```bash
mvn spring-boot:run
```

**Важно:** Убедитесь, что PostgreSQL полностью запущен перед запуском приложения. Приложение использует Liquibase для создания схемы БД при первом запуске.

Сервис будет доступен по адресу: `http://localhost:8080`

Swagger UI доступен по адресу: `http://localhost:8080/swagger-ui.html`

## Утилита для массового создания документов

Утилита `DocumentGeneratorUtil` создает N документов через API сервиса.

### Конфигурация

Создайте файл `src/main/resources/config.properties`:
```properties
# Количество документов для создания
N=100

# URL API сервиса
api.url=http://localhost:8080/api/documents

# Инициатор операций
initiator=document-generator
```

### Запуск утилиты

```bash
mvn exec:java -Dexec.mainClass="ru.itq.documents.service.util.DocumentGeneratorUtil"
```

Или через IDE запустите класс `DocumentGeneratorUtil`.

### Логирование прогресса

В логах будет видно:
- Общее количество документов для создания (N)
- Прогресс создания (каждые 100 документов)
- Время выполнения создания всех документов
- Среднее время создания одного документа

## Фоновые процессы (Workers)

В сервисе работают два фоновых процесса:

### 1. SUBMIT-worker
- Проверяет БД каждые 5 секунд (настраивается через `app.workers.submit-interval`)
- Находит документы со статусом DRAFT
- Отправляет их на согласование пачками по `batchSize` (по умолчанию 100)

### 2. APPROVE-worker
- Проверяет БД каждые 5 секунд (настраивается через `app.workers.approve-interval`)
- Находит документы со статусом SUBMITTED
- Отправляет их на утверждение пачками по `batchSize`

### Конфигурация workers

В `application.properties`:
```properties
app.workers.batch-size=100
app.workers.submit-interval=5000
app.workers.approve-interval=5000
app.workers.enabled=true
```

### Логирование фоновой обработки

В логах будет видно:
- Количество найденных документов для обработки
- Размер обрабатываемой пачки
- Время выполнения обработки пачки
- Количество успешно обработанных документов
- Количество документов с ошибками

Пример логов:
```
SubmitWorker: Found 150 DRAFT documents, processing batch of 100
SubmitWorker: Processed 100 documents in 234ms. Success: 98, Failed: 2
```

## Проверка прогресса по логам

### Создание документов
Ищите в логах:
- `"Created document {id} with number {number} by {initiator}"`
- `"Progress: {current}/{total} documents created"`
- `"Document generation completed: {created}/{total} documents created in {time}ms"`

### Фоновая обработка
Ищите в логах:
- `"SubmitWorker: Found {count} DRAFT documents"`
- `"SubmitWorker: Processed {count} documents in {time}ms"`
- `"ApproveWorker: Found {count} SUBMITTED documents"`
- `"ApproveWorker: Processed {count} documents in {time}ms"`

### Изменение статусов
Ищите в логах:
- `"Successfully transitioned document {id}: {fromStatus} -> {toStatus}"`
- `"Failed to transition document {id}: concurrent modification"`

## Тестирование

Запуск тестов:
```bash
mvn test
```

Реализованные тесты:
- ✅ Happy-path по одному документу
- ✅ Пакетный submit
- ✅ Пакетный approve с частичными результатами
- ✅ Откат approve при ошибке записи в реестр

## Структура базы данных

- `documents` - основная таблица документов
- `document_history` - история изменений статусов
- `approval_registry` - реестр утверждений

Миграции Liquibase находятся в `src/main/resources/db/changelog/`

## Обработка ошибок

Все ошибки возвращаются в едином формате:
```json
{
  "code": "ERROR_CODE",
  "message": "Error message"
}
```

Коды ошибок:
- `NOT_FOUND` - документ не найден
- `CONFLICT` - конфликт при изменении статуса (concurrent modification)
- `REGISTRY_ERROR` - ошибка записи в реестр утверждений
- `VALIDATION_ERROR` - ошибка валидации входных данных

## Оптимизация для обработки 5000+ ID

Для обработки больших пакетов (5000+ ID) рекомендуется:

1. **Разбиение на подпакеты**: Разделить список ID на пачки по 1000 элементов
2. **Асинхронная обработка**: Использовать `@Async` для параллельной обработки пачек
3. **Batch операции**: Использовать batch inserts для истории и реестра
4. **Connection pooling**: Увеличить размер пула соединений
5. **Индексы**: Убедиться в наличии индексов на часто используемых полях

## Вынос реестра утверждений в отдельную систему

Для выноса реестра утверждений в отдельную систему (отдельная БД или HTTP-сервис):

1. **Отдельная БД**:
   - Создать отдельный `DataSource` для реестра
   - Использовать `@Transactional` с указанием менеджера транзакций
   - Реализовать distributed transaction (JTA) или saga pattern

2. **HTTP-сервис**:
   - Создать клиент для вызова внешнего API
   - Использовать retry механизм для надежности
   - Реализовать компенсирующую транзакцию (откат статуса при ошибке)

Пример реализации с HTTP-сервисом:
```java
@Transactional
public BatchResult approveSingle(Long id, String initiator, String comment) {
    // Изменяем статус
    // ...
    
    try {
        // Вызов внешнего сервиса
        registryClient.registerApproval(id, initiator);
    } catch (Exception e) {
        // Откатываем статус
        repository.updateStatusOptimistic(id, APPROVED, SUBMITTED, version);
        throw new RegistryErrorException(e.getMessage());
    }
}
```

