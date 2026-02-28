# dynamodb-lib

A Kotlin library for DynamoDB operations with native cursor-based pagination support.

## Features

- ✅ Generic CRUD operations for DynamoDB entities
- ✅ Cursor-based pagination (native DynamoDB + in-memory sorting support)
- ✅ Async/await support via Kotlin coroutines
- ✅ Simple integration with Spring Boot
- ✅ Type-safe queries with builder pattern

## Installation

### Using JitPack

Add the JitPack repository to your `pom.xml`:

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```

Add the dependency:

```xml
<dependency>
  <groupId>com.github.SEU-USUARIO</groupId>
  <artifactId>dynamodb-lib</artifactId>
  <version>v1.0.0</version>
</dependency>
```

## Quick Start

### 1. Define your entity

```kotlin
import com.dynamodb.lib.entity.DynamoEntity
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

@DynamoDbBean
data class Order(
    @get:DynamoDbPartitionKey
    override var id: String? = null,
    var description: String? = null,
    override var createdAt: Long? = null,
    override var updatedAt: Long? = null
) : DynamoEntity
```

### 2. Create a repository

```kotlin
import com.dynamodb.lib.repository.BaseRepository
import com.dynamodb.lib.repository.SortOrder
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

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
    override fun applySorting(items: List<Order>, sortBy: String, sortOrder: SortOrder): List<Order> {
        val sorted = when (sortBy) {
            "description" -> items.sortedBy { it.description }
            "createdAt" -> items.sortedBy { it.createdAt }
            else -> items
        }
        return if (sortOrder == SortOrder.DESC) sorted.reversed() else sorted
    }

    override fun parseCursor(cursor: String, items: List<Order>): Int {
        val cursorId = cursor.extractCursorId() ?: return 0
        return items.indexOfFirst { it.id == cursorId }.takeIf { it >= 0 }?.plus(1) ?: 0
    }

    override fun encodeCursor(item: Order): String? = item.id?.toCursor()
}
```

### 3. Use in service

```kotlin
import org.springframework.stereotype.Service

@Service
class OrderService(val repository: OrderRepository) {
    suspend fun findAll(limit: Int, cursor: String?, sortBy: String?) =
        repository.getAllPaginated(
            limit = limit,
            cursor = cursor,
            sortBy = sortBy
        )
}
```

## Pagination

The library supports cursor-based pagination which is the industry standard for DynamoDB:

### Without sorting (efficient, O(limit))

```kotlin
val page = repository.getAllPaginated(limit = 10)
// Response: PageResult with items, nextCursor, hasNextPage flags
```

### With sorting (O(n), loads all items in memory)

```kotlin
val page = repository.getAllPaginated(
    limit = 10,
    sortBy = "createdAt",
    sortOrder = SortOrder.DESC
)
```

### Navigation

```kotlin
// First page
val firstPage = repository.getAllPaginated(limit = 10)

// Next page
val nextPage = repository.getAllPaginated(
    limit = 10,
    cursor = firstPage.nextCursor
)

// Check flags
if (firstPage.hasNextPage) { /* show next button */ }
if (page.hasPreviousPage) { /* show previous button */ }
```

## License

MIT