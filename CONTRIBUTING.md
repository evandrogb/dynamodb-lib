# Contribuindo para dynamodb-lib

Obrigado por querer contribuir! Aqui estão as diretrizes.

## 🚀 Quick Start

```bash
# 1. Clone
git clone https://github.com/evandrogb/dynamodb-lib.git
cd dynamodb-lib

# 2. Crie uma branch
git checkout -b feature/seu-feature

# 3. Edite, compile e teste
mvn clean test

# 4. Commit
git commit -m "feat: descrição do que fez"

# 5. Push
git push origin feature/seu-feature

# 6. Abra um Pull Request
```

## 📝 Commits

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat:    Nova feature
fix:     Correção de bug
docs:    Documentação
refactor: Refatoração sem mudança de behavior
test:    Adição de testes
chore:   Mudanças de build, dependencies, etc
```

**Exemplos:**
```bash
git commit -m "feat: add pagination with sorting support"
git commit -m "fix: cursor deserialization error"
git commit -m "docs: update README with examples"
```

## 🧪 Testes

Sempre teste antes de fazer commit:

```bash
# Testes completos
mvn clean test

# Teste específico
mvn test -Dtest=OrderRepositoryPaginationTest

# Sem testes (rápido)
mvn clean package -DskipTests
```

**Esperado:**
```
Tests run: 18, Failures: 0, Errors: 0
BUILD SUCCESS
```

## 📂 Estrutura de Código

```
src/main/kotlin/com/dynamodb/lib/
├── entity/          ← Interfaces base
├── model/           ← Data classes (PageResult, Query, etc)
├── repository/      ← Implementação (DynamoRepository, BaseRepository)
└── builder/         ← DSL e builders
```

## ✍️ Estilo de Código

- Use Kotlin idiomático (data classes, extension functions, etc)
- Máximo 120 caracteres de linha
- 2 espaços de indentação
- Coloque javadoc em classes/funções públicas

```kotlin
/**
 * Descrição da função
 *
 * @param limit Quantos itens retornar
 * @return PageResult com os itens paginados
 */
suspend fun getAllPaginated(limit: Int): PageResult<T>
```

## 🔀 Pull Requests

Ao abrir um PR:

1. **Título claro:** `feat: add sorting support`
2. **Descrição completa:** O que foi mudado e por quê
3. **Referência issue:** `Fixes #123` (se aplicável)
4. **Testes passando:** Verde ✅ no CI

**Exemplo:**
```markdown
## Descrição

Adiciona suporte a paginação com sorting em memória.

## Mudanças

- [x] Adicionado método `applySorting()`
- [x] Atualizado `BaseRepository.kt`
- [x] Testes adicionados para sorted pagination

## Testes

- 18/18 testes passam
- Novo teste: `testSortedPaginationWithCursor`

Fixes #45
```

## 📚 Documentação

Se adicionar feature nova:

1. Atualize `README.md` com exemplo de uso
2. Adicione javadoc no código
3. Atualize `DEVELOPMENT.md` se for desenvolvimento relevante

## 🐛 Reportando Bugs

Se encontrar bug:

1. Abra uma [Issue](https://github.com/evandrogb/dynamodb-lib/issues)
2. Descreva o comportamento esperado vs observado
3. Forneça minimal reproducible example

```markdown
## Bug Description

BaseRepository lança NPE ao usar cursor nulo

## Steps to Reproduce

1. Chamar `getAllPaginated(limit=10, cursor=null)`
2. Verificar exception

## Expected Behavior

Deve retornar primeira página sem erro

## Actual Behavior

NullPointerException em CursorExtensions.kt:45
```

## 🎯 Roadmap

Ideias para contribuições:

- [ ] Query Filters (WHERE clauses)
- [ ] Batch operations melhoradas
- [ ] Suporte a transações
- [ ] Projections (select specific fields)
- [ ] Event listeners (pre/post save, delete)

Abra uma Issue se quiser trabalhar em algo!

## 📦 Release Process

(Apenas para maintainers)

```bash
# 1. Atualizar versão em pom.xml
# 2. Commit: "chore: bump version to 1.0.1"
# 3. Tag: git tag v1.0.1
# 4. Push: git push origin main && git push origin v1.0.1
# 5. JitPack: Clique "Get it" em https://jitpack.io
```

## ✨ Código de Conduta

- Seja respeitoso
- Ofereça feedback construtivo
- Foque na ideia, não na pessoa
- Respeite diversidade

## 🤝 Precisa de Ajuda?

- Abra uma [Discussion](https://github.com/evandrogb/dynamodb-lib/discussions)
- Veja `DEVELOPMENT.md` para setup local
- Abra uma Issue com `question` label

---

**Obrigado por contribuir! 🙌**