# Order Service

Сервис обработки заказов на Spring Boot. Хранит заказы в PostgreSQL (миграции через Flyway),
после создания заказа кидает событие в RabbitMQ.

## Запуск

Понадобится Docker и JDK 17+.

Поднимаем базу и брокер:

```bash
docker compose up -d
```

Запускаем приложение:

```bash
./gradlew bootRun
```

Что где живёт:

- Swagger UI: http://localhost:8080/swagger-ui.html
- Веб-морда (Thymeleaf): http://localhost:8080/ui/orders
- RabbitMQ консоль: http://localhost:15672, логин/пароль guest/guest
- Postgres поднимается на порту 5433

Тесты гоняются через Testcontainers, так что Docker для них тоже нужен:

```bash
./gradlew test
```

## Архитектура

Слои обычные: контроллер принимает запрос, сервис содержит логику, репозиторий (Spring Data JPA)
ходит в Postgres. Заказ (Order) и его позиции (OrderItem) связаны как один ко многим.

Когда создаётся заказ, сервис кладёт в очередь order.created JSON с полями orderId, customerName
и totalAmount. Слушатель забирает сообщение, пишет лог и переводит заказ в статус PROCESSING.
Получается сквозная цепочка: REST -> база -> очередь -> слушатель -> обновление статуса.

Сумма всех заказов клиента считается отдельным JPQL-запросом с JOIN и агрегацией
SUM(price * quantity). Поля, по которым фильтруем (status, customer_name), вынесены в индексы.
Ошибки ловит @RestControllerAdvice и отдаёт нормальные коды (400, 404) с понятным телом.

## Примеры запросов

Создать заказ:

```bash
curl -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" \
  -d '{"customerName":"Alice","items":[{"productName":"Laptop","quantity":1,"price":1500.00}]}'
```

Список заказов с фильтром по статусу, пагинацией и сортировкой:

```bash
curl "http://localhost:8080/api/orders?status=PROCESSING&page=0&size=10&sort=orderDate,desc"
```

Получить заказ по id вместе с позициями:

```bash
curl http://localhost:8080/api/orders/<id>
```

Поменять статус:

```bash
curl -X PUT http://localhost:8080/api/orders/<id>/status -H "Content-Type: application/json" \
  -d '{"status":"COMPLETED"}'
```

Общая сумма заказов клиента:

```bash
curl "http://localhost:8080/api/orders/total?customerName=Alice"
```
