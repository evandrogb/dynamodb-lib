# Desenvolvimento da dynamodb-lib

Guia completo para desenvolver, testar e publicar a biblioteca localmente e no JitPack.

## 📋 Sumário

- [Setup Local](#setup-local)
- [Testar Localmente](#testar-localmente)
- [Atualizar JitPack](#atualizar-jitpack)
- [Debug com Projeto Principal](#debug-com-projeto-principal)

---

## Setup Local

### 1. Clonar o Repositório

```bash
git clone https://github.com/evandrogb/dynamodb-lib.git
cd dynamodb-lib
```

### 2. Compilar a Biblioteca

```bash
mvn clean install -DskipTests
```

Isso compila e instala em `~/.m2/repository/com/dynamodb/dynamodb-lib/1.0.0/`

### 3. Verificar a Instalação

```bash
ls ~/.m2/repository/com/dynamodb/dynamodb-lib/1.0.0/
# Deve listar: dynamodb-lib-1.0.0.jar, dynamodb-lib-1.0.0.pom, etc
```

---

## Testar Localmente

Você tem 3 opções para testar a lib localmente enquanto desenvolve:

### ✅ Opção 1: Maven Install (Recomendado)

**Melhor para:** Desenvolvimento iterativo + debug normal

```bash
# 1. Editar código da lib
# 2. Recompilar e instalar
cd /Users/evandro/Projects/library/dynamodb-lib
mvn clean install -DskipTests

# 3. No projeto principal, usar normalmente
cd /Users/evandro/Downloads/dynamodb
mvn test
```

O projeto principal automaticamente pega a nova versão de `~/.m2`

**Vantagem:**
- ✅ Simples e direto
- ✅ Maven gerencia tudo
- ✅ Funciona em qualquer IDE

**Desvantagem:**
- Precisa recompilar a lib a cada mudança

---

### 🔗 Opção 2: Apontar para o JAR Local

**Melhor para:** Testes rápidos sem recompilar

No `pom.xml` do projeto principal (`/Users/evandro/Downloads/dynamodb`):

```xml
<dependency>
    <groupId>com.dynamodb</groupId>
    <artifactId>dynamodb-lib</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>/Users/evandro/Projects/library/dynamodb-lib/target/dynamodb-lib-1.0.0.jar</systemPath>
</dependency>
```

**Passos:**
1. Compilar a lib uma vez: `mvn clean package`
2. Apontar com `systemPath` no pom.xml
3. Rodar testes no projeto principal

**Vantagem:**
- ✅ Aponta direto pro JAR já compilado
- ✅ Mais rápido que install

**Desvantagem:**
- Caminho absoluto (não portável)
- Precisa compilar a lib manualmente

---

### 🐛 Opção 3: Multi-Module no IntelliJ (Melhor Debug)

**Melhor para:** Debug em tempo real atravessando lib ↔ projeto

#### Configuração:

1. **Abra o projeto principal no IntelliJ:**
   ```bash
   open -a "IntelliJ IDEA" /Users/evandro/Downloads/dynamodb
   ```

2. **Importe a lib como módulo:**
   - `File` → `Project Structure` → `Modules`
   - Clique `+` (Add) → `Import Module`
   - Selecione: `/Users/evandro/Projects/library/dynamodb-lib`
   - Marque: `Import module from external model` → `Maven`

3. **IntelliJ automaticamente:**
   - Detecta a lib como dependência
   - Permite debug atravessando módulos
   - Recompila a lib ao fazer mudanças

#### Usando:

```bash
# Terminal: Watch na lib (recompila automaticamente)
cd /Users/evandro/Projects/library/dynamodb-lib
mvn compile

# IntelliJ: Run → Debug Tests
# Você pode debugar código da lib em tempo real!
```

**Vantagem:**
- ✅ Debug em tempo real atravessando modules
- ✅ Modificações na lib refletem imediatamente
- ✅ Melhor experiência de desenvolvimento

**Desvantagem:**
- Precisa de IntelliJ/IDE
- Um pouco mais complexo de configurar

---

## Atualizar JitPack

Quando você terminar de desenvolver e quer publicar uma nova versão:

### 1. Versão Local Testada

```bash
# Garantir que está compilando e testando
cd /Users/evandro/Projects/library/dynamodb-lib
mvn clean test
# Resultado esperado: BUILD SUCCESS
```

### 2. Commit e Push para GitHub

```bash
git add .
git commit -m "feat: [descrição da mudança]"
git push origin main
```

### 3. Criar Nova Tag (para nova versão)

Se for lançar versão `v1.0.1`:

```bash
git tag v1.0.1
git push origin v1.0.1
```

### 4. Ativar no JitPack

1. Acesse: https://jitpack.io/#evandrogb/dynamodb-lib
2. Procure a nova tag (ex: `v1.0.1`)
3. Clique **Get it** e aguarde compilar (2-3 minutos)

### 5. Usar a Nova Versão no Projeto Principal

Atualize o `pom.xml` do projeto:

```xml
<dependency>
    <groupId>com.github.evandrogb</groupId>
    <artifactId>dynamodb-lib</artifactId>
    <version>v1.0.1</version>  <!-- Alterado -->
</dependency>
```

---

## Debug com Projeto Principal

### Cenário: Encontrou um Bug no BaseRepository.kt

**Com Opção 1 (Maven Install):**

```bash
# 1. Editar BaseRepository.kt
# 2. Testar localmente
cd /Users/evandro/Projects/library/dynamodb-lib
mvn test -Dtest=BaseRepositoryTest

# 3. Instalar nova versão
mvn clean install -DskipTests

# 4. Testar no projeto principal
cd /Users/evandro/Downloads/dynamodb
mvn test
```

**Com Opção 3 (IntelliJ Multi-Module):**

1. Abra a classe em BaseRepository.kt
2. Clique `Debug` → `Debug 'OrderRepositoryPaginationTest'`
3. Coloque breakpoint em BaseRepository.kt
4. Step through do teste!

---

## Workflow Recomendado

```
┌─────────────────────────────────────────────────────┐
│  1. DESENVOLVIMENTO LOCAL (Opção 1 ou 3)            │
│  ├─ Editar código da lib                            │
│  ├─ mvn clean install -DskipTests                   │
│  └─ mvn test (no projeto principal)                 │
├─────────────────────────────────────────────────────┤
│  2. VALIDAR                                         │
│  ├─ cd /Users/evandro/Projects/library/dynamodb-lib │
│  ├─ mvn clean test (18/18 deve passar)              │
│  └─ Verificar se não quebrou nada                   │
├─────────────────────────────────────────────────────┤
│  3. PUBLICAR (Quando pronto)                        │
│  ├─ git add . && git commit -m "..."                │
│  ├─ git tag v1.0.X                                  │
│  ├─ git push origin main && git push origin v1.0.X  │
│  └─ Ativar em: https://jitpack.io/#evandrogb/...    │
└─────────────────────────────────────────────────────┘
```

---

## Estrutura da Biblioteca

```
dynamodb-lib/
├── src/main/kotlin/com/dynamodb/lib/
│   ├── entity/
│   │   └── DynamoEntity.kt          ← Interfaces base
│   ├── model/
│   │   └── PageResult.kt            ← Resultado paginado
│   ├── repository/
│   │   ├── DynamoRepository.kt      ← Interface CRUD
│   │   ├── BaseRepository.kt        ← Implementação genérica
│   │   └── CursorExtensions.kt      ← Utilitários de cursor
│   └── builder/
│       └── QueryBuilder.kt          ← DSL para queries
├── pom.xml                          ← Dependências Maven
├── README.md                        ← Uso da lib
└── DEVELOPMENT.md                   ← Este arquivo
```

---

## Comandos Úteis

```bash
# Compilar
mvn clean compile

# Testar
mvn test

# Instalar localmente
mvn clean install

# Compilar sem testes
mvn clean package -DskipTests

# Ver estrutura de dependências
mvn dependency:tree

# Executar teste específico
mvn test -Dtest=OrderRepositoryPaginationTest

# Limpar cache local
rm -rf ~/.m2/repository/com/dynamodb/dynamodb-lib/
```

---

## Troubleshooting

### ❌ "Tests fail after editing BaseRepository"

```bash
# 1. Limpar cache
rm -rf ~/.m2/repository/com/dynamodb/dynamodb-lib/

# 2. Recompilar e instalar
mvn clean install

# 3. Testar novamente
mvn test
```

### ❌ "IntelliJ não reconhece mudanças na lib"

```bash
# Build → Rebuild Project
# Ou pressione: Cmd + Shift + K (macOS)
```

### ❌ "JitPack com erro '401 Unauthorized'"

- Verifique se o repositório está **PUBLIC** em Settings
- Aguarde 1-2 minutos e tente novamente

### ❌ "systemPath não funciona"

- Use apenas para debug local
- Para publicar, sempre use Maven Install ou JitPack

---

## Links Úteis

| Recurso | URL |
|---------|-----|
| **GitHub Repo** | https://github.com/evandrogb/dynamodb-lib |
| **JitPack Status** | https://jitpack.io/#evandrogb/dynamodb-lib |
| **Projeto Principal** | /Users/evandro/Downloads/dynamodb |
| **Lib Local** | /Users/evandro/Projects/library/dynamodb-lib |
| **Maven Local** | ~/.m2/repository/com/dynamodb/dynamodb-lib/ |

---

## FAQ

**P: Preciso editar a lib, qual opção usar?**
R: Use Opção 1 (Maven Install) para simplicidade, ou Opção 3 (IntelliJ) para melhor debug.

**P: Quantas vezes preciso recompilar?**
R: A cada mudança no código da lib que queira testar no projeto principal.

**P: Posso debugar sem IntelliJ?**
R: Sim! Use Maven com `mvn -X` para mais informações, ou adicione print statements.

**P: Qual versão usar no projeto principal?**
R: Use `1.0.0` (local) para desenvolvimento, `v1.0.0` (JitPack) para produção.

---

## Versionamento

- **1.0.0** = Versão local no ~/.m2
- **v1.0.0** = Tag no GitHub para JitPack

Sempre use `v` (com v) para tags no GitHub!

---

**Última atualização:** 2026-02-28
**Status:** Desenvolvimento ativo
