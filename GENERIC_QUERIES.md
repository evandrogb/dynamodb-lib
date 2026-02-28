# 🎯 Métodos Genéricos de Busca

A `dynamodb-lib` agora oferece métodos genéricos e poderosos para buscas que funcionam com **QUALQUER entidade** que implemente `DynamoEntity`.

## 📚 Índice

- [Busca por Propriedade](#busca-por-propriedade)
- [Scan com Filtro](#scan-com-filtro)
- [Scan Paginado com Filtro](#scan-paginado-com-filtro)
- [Exemplos de Uso](#exemplos-de-uso)
- [Performance](#performance)
- [Best Practices](#best-practices)

---

## Busca por Propriedade

### `getByProperty(propertyName, value)`

Busca **UM** item por uma propriedade específica.

```kotlin
// Buscar um usuário por email
val user = userRepository.getByProperty("email", "user@example.com")

// Buscar um produto por SKU
val product = productRepository.getByProperty("sku", "PROD-123")

// Buscar um order por numero
val order = orderRepository.getByProperty("orderNumber", "ORD-2026-001")
```

**Assinatura:**
```kotlin
suspend fun getByProperty(propertyName: String, value: Any): T?
```

**Retorna:** A primeira entidade que corresponde ao filtro, ou `null`

---

### `getAllByProperty(propertyName, value)`

Busca **TODOS** os items por uma propriedade específica.

```kotlin
// Buscar todos os orders do cliente
val customerOrders = orderRepository.getAllByProperty("customerId", "CUST-123")

// Buscar todos os usuários ativos
val activeUsers = userRepository.getAllByProperty("status", "ACTIVE")

// Buscar todos os pedidos cancelados
val cancelledOrders = orderRepository.getAllByProperty("status", "CANCELLED")
```

**Assinatura:**
```kotlin
suspend fun getAllByProperty(propertyName: String, value: Any): List<T>
```

**Retorna:** Lista de todas as entidades que correspondem ao filtro

---

## Scan com Filtro

### `scanWithFilter(predicate)`

Busca items usando uma **função lambda** para mais flexibilidade.

```kotlin
// Buscar orders recentes (últimas 24h)
val recentOrders = orderRepository.scanWithFilter { order ->
    (order.createdAt ?: 0) >= System.currentTimeMillis() - 24 * 60 * 60 * 1000
}

// Buscar usuários com nome começando com "A"
val usersStartingA = userRepository.scanWithFilter { user ->
    user.name?.startsWith("A", ignoreCase = true) ?: false
}

// Buscar products caros (> 1000)
val expensive = productRepository.scanWithFilter { product ->
    product.price > 1000
}

// Buscar com múltiplas condições
val activeExpensive = productRepository.scanWithFilter { product ->
    product.status == "ACTIVE" && product.price > 500
}
```

**Assinatura:**
```kotlin
suspend fun scanWithFilter(predicate: (T) -> Boolean): List<T>
```

**Retorna:** Lista de items que satisfazem o predicado

**Vantagens:**
- ✅ Mais flexível que `getByProperty()`
- ✅ Suporta lógica complexa
- ✅ Comparações (>, <, >=, <=, contains, etc)

**Desvantagens:**
- ❌ Menos performático (carrega tudo e filtra em memória)
- ❌ Não pode usar índices do DynamoDB

---

## Scan Paginado com Filtro

### `scanWithFilterPaginated(limit, cursor, predicate)`

Busca items com **filtro E paginação**.

```kotlin
// Buscar orders ativos com paginação
val page1 = orderRepository.scanWithFilterPaginated(
    limit = 20,
    cursor = null,  // Primeira página
    predicate = { order -> order.status == "ACTIVE" }
)

// Navegar para próxima página
val page2 = orderRepository.scanWithFilterPaginated(
    limit = 20,
    cursor = page1.nextCursor,
    predicate = { order -> order.status == "ACTIVE" }
)

// Buscar com filtro complexo
val filtered = orderRepository.scanWithFilterPaginated(
    limit = 15,
    cursor = null,
    predicate = { order ->
        order.status == "PENDING" &&
        (order.createdAt ?: 0) >= cutoffTime &&
        order.amount > 100
    }
)

println("Items: ${filtered.itemCount}/${filtered.totalItems}")
println("Tem próxima página? ${filtered.hasNextPage}")
```

**Assinatura:**
```kotlin
suspend fun scanWithFilterPaginated(
    limit: Int,
    cursor: String?,
    predicate: (T) -> Boolean
): PageResult<T>
```

**Retorna:** `PageResult<T>` com:
- `items`: Items da página atual (filtrados)
- `itemCount`: Quantos items na página
- `totalItems`: Total de items na tabela (antes do filtro)
- `nextCursor`: Para próxima página
- `previousCursor`: Para página anterior
- `hasNextPage`: Flag booleana
- `hasPreviousPage`: Flag booleana

---

## Exemplos de Uso

### Cenário 1: Repository Customizado para Order

```kotlin
@Repository
class OrderRepository(
    enhancedClient: DynamoDbEnhancedAsyncClient,
    dynamoClient: DynamoDbAsyncClient
) : BaseRepository<Order>(
    tableName = "orders",
    entityClass = Order::class.java,
    enhancedClient = enhancedClient,
    dynamoClient = dynamoClient
) {
    // ✨ Lógica específica de Order

    override fun applySorting(items: List<Order>, sortBy: String, sortOrder: SortOrder): List<Order> {
        val sorted = when (sortBy.lowercase()) {
            "id" -> items.sortedWith(compareBy { it.id })
            "amount" -> items.sortedBy { it.amount }
            else -> items
        }
        return if (sortOrder == SortOrder.DESC) sorted.reversed() else sorted
    }

    // 🎯 Queries de negócio usando métodos genéricos

    suspend fun getOrdersByStatus(status: String) =
        getAllByProperty("status", status)

    suspend fun getOrdersByCustomer(customerId: String) =
        getAllByProperty("customerId", customerId)

    suspend fun getRecentOrders(days: Int = 7): List<Order> {
        val cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
        return scanWithFilter { order -> (order.createdAt ?: 0) >= cutoff }
    }

    suspend fun getExpensiveOrders(minAmount: Double): List<Order> {
        return scanWithFilter { order -> order.amount >= minAmount }
    }

    suspend fun searchByDescription(text: String): List<Order> {
        return scanWithFilter { order ->
            order.description?.contains(text, ignoreCase = true) ?: false
        }
    }

    // 🔧 CRUD base vem automaticamente da lib!
    // - save()
    // - getById()
    // - update()
    // - deleteById()
    // - getAll()
    // - getAllPaginated()
    // - count()
}
```

### Cenário 2: Service Customizado

```kotlin
@Service
class OrderService(
    private val orderRepository: OrderRepository
) {
    // CRUD básico
    suspend fun createOrder(order: Order) = orderRepository.save(order)
    suspend fun getOrder(id: String) = orderRepository.getById(id)
    suspend fun updateOrder(order: Order) = orderRepository.update(order)
    suspend fun deleteOrder(id: String) = orderRepository.deleteById(id)

    // Queries de negócio
    suspend fun getCustomerOrders(customerId: String) =
        orderRepository.getOrdersByCustomer(customerId)

    suspend fun getPendingOrders() =
        orderRepository.getOrdersByStatus("PENDING")

    suspend fun getRecentOrders() =
        orderRepository.getRecentOrders(days = 7)

    suspend fun findExpensive(minAmount: Double) =
        orderRepository.getExpensiveOrders(minAmount)

    suspend fun search(description: String) =
        orderRepository.searchByDescription(description)

    // Paginação com filtro
    suspend fun getPendingOrdersPaginated(limit: Int, cursor: String?) =
        orderRepository.scanWithFilterPaginated(
            limit = limit,
            cursor = cursor,
            predicate = { it.status == "PENDING" }
        )
}
```

### Cenário 3: Controller com Paginação e Filtros

```kotlin
@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService
) {
    @GetMapping
    suspend fun listOrders(
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) status: String?
    ): PageResult<Order> {
        return if (status != null) {
            // Filtro por status com paginação
            orderService.orderRepository.scanWithFilterPaginated(
                limit = limit.coerceIn(1, 100),
                cursor = cursor,
                predicate = { it.status == status }
            )
        } else {
            // Paginação padrão
            orderService.orderRepository.getAllPaginated(limit)
        }
    }

    @GetMapping("/expensive")
    suspend fun getExpensive(
        @RequestParam(defaultValue = "1000") minAmount: Double
    ): List<Order> {
        return orderService.findExpensive(minAmount)
    }

    @GetMapping("/search")
    suspend fun search(
        @RequestParam query: String,
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(required = false) cursor: String?
    ): PageResult<Order> {
        return orderService.orderRepository.scanWithFilterPaginated(
            limit = limit.coerceIn(1, 100),
            cursor = cursor,
            predicate = { it.description?.contains(query, ignoreCase = true) ?: false }
        )
    }
}
```

---

## Performance

### Complexidade de Tempo

| Método | Complexidade | Índice Needed |
|--------|---------|---|
| `getById()` | O(1) | ✅ Partition Key |
| `getByProperty()` | O(n) | ⚠️ Recomendado |
| `getAllByProperty()` | O(n) | ⚠️ Recomendado |
| `scanWithFilter()` | O(n) | ❌ Não |
| `scanWithFilterPaginated()` | O(n) | ❌ Não |
| `getAllPaginated()` | O(limit) | ✅ Nativo (sem sort) |

**Legenda:**
- ✅ Muito rápido
- ⚠️ Recomenda índice
- ❌ Carrega tudo em memória

---

### Tips de Performance

1. **Use `getByProperty()` se a propriedade tiver índice:**
   ```kotlin
   // Bom - email tem índice
   val user = userRepository.getByProperty("email", "user@example.com")

   // Ruim - sem índice, precisa fazer scan
   val user = userRepository.getByProperty("phone", "+55-11-99999")
   ```

2. **Para filtros simples use `getByProperty()`, não `scanWithFilter()`:**
   ```kotlin
   // ✅ Melhor
   val users = userRepository.getAllByProperty("status", "ACTIVE")

   // ❌ Menos eficiente
   val users = userRepository.scanWithFilter { it.status == "ACTIVE" }
   ```

3. **Para filtros complexos use `scanWithFilter()`:**
   ```kotlin
   // ✅ Correto para múltiplas condições
   val users = userRepository.scanWithFilter { user ->
       user.status == "ACTIVE" &&
       user.age >= 18 &&
       user.country == "BR"
   }
   ```

4. **Sempre pagine dados grandes:**
   ```kotlin
   // ✅ Bom
   val page = userRepository.scanWithFilterPaginated(
       limit = 50,
       cursor = null,
       predicate = { it.status == "ACTIVE" }
   )

   // ❌ Ruim - pode consumir muita memória
   val allActive = userRepository.scanWithFilter { it.status == "ACTIVE" }
   ```

---

## Best Practices

### 1. **Repository = Data Access, Service = Business Logic**

```kotlin
// ✅ Bom
@Repository
class UserRepository : BaseRepository<User> {
    suspend fun getUserByEmail(email: String) = getByProperty("email", email)
    // Apenas métodos de acesso a dados!
}

@Service
class UserService(private val userRepository: UserRepository) {
    suspend fun findUserByEmail(email: String) = userRepository.getUserByEmail(email)
    suspend fun validateUser(user: User) { /* lógica de negócio */ }
    // Lógica de negócio fica aqui!
}

// ❌ Ruim
@Service
class UserService {
    suspend fun findUserByEmail(email: String) {
        // Scan direto sem repository
    }
}
```

### 2. **Documentar Índices Necessários**

```kotlin
@Repository
class OrderRepository : BaseRepository<Order> {
    // Requer: Global Secondary Index em "customerId"
    suspend fun getOrdersByCustomer(customerId: String) =
        getAllByProperty("customerId", customerId)

    // Requer: Global Secondary Index em "status"
    suspend fun getOrdersByStatus(status: String) =
        getAllByProperty("status", status)
}
```

### 3. **Cache Resultados Frequentes**

```kotlin
@Repository
class UserRepository : BaseRepository<User> {
    @Cacheable(value = ["active-users"])
    suspend fun getActiveUsers(): List<User> {
        return getAllByProperty("status", "ACTIVE")
    }
}
```

### 4. **Validar Entrada**

```kotlin
suspend fun searchUsers(query: String) {
    require(query.isNotBlank() && query.length >= 3) {
        "Query must be at least 3 characters"
    }
    return userRepository.scanWithFilter { user ->
        user.name.contains(query, ignoreCase = true)
    }
}
```

---

## Resumo

| Método | Use Para | Performance |
|--------|----------|---------|
| `getById(id)` | Busca por ID (chave primária) | ⚡ Ótima |
| `getByProperty(prop, val)` | Busca um item por propriedade | 🟡 Média (com índice) |
| `getAllByProperty(prop, val)` | Busca vários items por propriedade | 🟡 Média (com índice) |
| `scanWithFilter(lambda)` | Filtros complexos | 🔴 Lenta (sem índice) |
| `scanWithFilterPaginated(...)` | Filtros + paginação | 🔴 Lenta (sem índice) |
| `getAllPaginated(...)` | Paginação simples | ⚡ Ótima (nativa) |

---

**Última atualização:** 2026-02-28
**Versão:** dynamodb-lib 1.0.0+
