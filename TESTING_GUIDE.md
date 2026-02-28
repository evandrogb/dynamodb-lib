# Guia Rápido de Testes

Como testar a `dynamodb-lib` localmente enquanto desenvolve.

## 🎯 Escolha Seu Método

```
┌──────────────────────────────────────────────────────────────────┐
│  Qual é seu caso de uso?                                         │
└──────────────────────────────────────────────────────────────────┘

  🟢 Desenvolvimento rápido          → USE OPÇÃO 1
    ├─ Editar código frequentemente
    └─ Precisa testar rápido

  🟡 Debug detalhado                → USE OPÇÃO 3
    ├─ Debugar código passo a passo
    ├─ Ver o que acontece em cada linha
    └─ Atravessar lib → projeto

  🔴 Teste rápido sem recompilar   → USE OPÇÃO 2
    ├─ JAR já compilado
    ├─ Quer só testar
    └─ Não vai editar código
```

---

## ✅ OPÇÃO 1: Maven Install (Recomendado)

**Para:** Desenvolvimento iterativo rápido

### Setup (1 vez)

```bash
# Ir para a lib
cd /Users/evandro/Projects/library/dynamodb-lib

# Compilar primeira vez
mvn clean install -DskipTests

# ✅ Pronto! Instalado em ~/.m2
```

### Workflow (a cada mudança)

```bash
# 1. Editar código em BaseRepository.kt ou qualquer classe

# 2. Recompilar e instalar
mvn clean install -DskipTests

# 3. Ir para projeto principal
cd /Users/evandro/Downloads/dynamodb

# 4. Rodar testes
mvn test

# ✅ Projeto principal usa nova versão automaticamente!
```

### Terminal Real

```bash
# Terminal 1: Na lib
$ cd /Users/evandro/Projects/library/dynamodb-lib
$ mvn clean install -DskipTests
[INFO] Building jar: target/dynamodb-lib-1.0.0.jar
[INFO] Installing to ~/.m2/repository/com/dynamodb/dynamodb-lib/1.0.0/
[INFO] BUILD SUCCESS

# Terminal 2: No projeto (em paralelo)
$ cd /Users/evandro/Downloads/dynamodb
$ mvn test
[INFO] Tests run: 18, Failures: 0
[INFO] BUILD SUCCESS
```

### Prós & Contras

✅ **Prós:**
- Simples e direto
- Maven cuida de tudo
- Funciona em qualquer IDE

❌ **Contras:**
- Precisa recompilar a cada mudança (~5-10s)

---

## 🔗 OPÇÃO 2: SystemPath (Teste Rápido)

**Para:** Quando JAR já está compilado

### Setup

1. **Compilar a lib uma vez:**

```bash
cd /Users/evandro/Projects/library/dynamodb-lib
mvn clean package -DskipTests
```

2. **Editar `pom.xml` do projeto principal:**

```xml
<dependency>
    <groupId>com.dynamodb</groupId>
    <artifactId>dynamodb-lib</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>/Users/evandro/Projects/library/dynamodb-lib/target/dynamodb-lib-1.0.0.jar</systemPath>
</dependency>
```

3. **Testar:**

```bash
cd /Users/evandro/Downloads/dynamodb
mvn test
```

### Prós & Contras

✅ **Prós:**
- Aponta direto pro JAR
- Sem install em ~/.m2
- Teste isolado

❌ **Contras:**
- Caminho absoluto (não portável)
- Precisa compilar JAR manualmente se editar lib
- ⚠️ Não recomendado para produção

### Quando Usar

```bash
# Só quando quiser testar JAR já pronto
# Sem fazer mais edições na lib

mvn clean package -DskipTests
# ... editar projeto principal ...
mvn test
```

---

## 🐛 OPÇÃO 3: Multi-Module no IntelliJ (Melhor Debug)

**Para:** Debug profundo e desenvolvimento avançado

### Setup (1 vez)

1. **Abra projeto principal no IntelliJ:**

```bash
open -a "IntelliJ IDEA" /Users/evandro/Downloads/dynamodb
```

2. **Importe módulo da lib:**

Menu: `File` → `Project Structure` → `Modules`

```
+ (Add) → Import Module
  ↓
Selecione: /Users/evandro/Projects/library/dynamodb-lib
  ↓
Marque: "Import module from external model" → Maven
  ↓
OK
```

3. **IntelliJ auto-reconhece:**
   - ✅ Detecta lib como dependência
   - ✅ Abre código-fonte da lib
   - ✅ Permite debug atravessando módulos

### Workflow

**Terminal:**
```bash
cd /Users/evandro/Projects/library/dynamodb-lib
mvn compile
```

**IntelliJ:**
1. Clique em qualquer teste → `Run` ou `Debug`
2. Coloque breakpoint em `BaseRepository.kt`
3. Execute teste → Para no breakpoint
4. Step into/over/continue normalmente

### Exemplo Visual

```
Teste:                         BaseRepository:
┌──────────────────┐          ┌────────────────────┐
│ @Test            │          │ override fun        │
│ fun testPaging() ├─────────→ │ applySorting()     │
│ {                │  (debug)  │ {                  │
│   breakpoint ●   │           │   ● breakpoint     │
│ }                │           │   items.sorted...  │
└──────────────────┘          └────────────────────┘
        ↓
    Step over/into
        ↓
   Ver valores em tempo real!
```

### Prós & Contras

✅ **Prós:**
- Debug em tempo real
- Ver valores de variáveis
- Step through código
- Melhor experiência de dev

❌ **Contras:**
- Precisa de IntelliJ
- Setup mais complexo

---

## 📊 Comparação Rápida

| Aspecto | Opção 1 | Opção 2 | Opção 3 |
|---------|---------|---------|---------|
| **Facilidade** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Velocidade** | Média | Rápida | Mais lenta (debug) |
| **Debug** | Difícil | Médio | Excelente |
| **Portável** | ✅ Sim | ❌ Não | ✅ Sim |
| **Recomendado** | ✅ | ⚠️ | ✅ |

---

## 🔄 Workflow Dia a Dia

### Dia 1: Desenvolvimento Normal

```bash
# Terminal 1: Watch na lib
cd /Users/evandro/Projects/library/dynamodb-lib
mvn clean install -DskipTests

# Terminal 2: Testes no projeto
cd /Users/evandro/Downloads/dynamodb
mvn test

# Editar código... recompilar... testar...
# (Repete conforme necessário)
```

### Dia 2: Debug de Bug Complexo

```bash
# Abrir IntelliJ (Opção 3)
open -a "IntelliJ IDEA" /Users/evandro/Downloads/dynamodb

# Colocar breakpoint em BaseRepository.kt
# Debugar teste passo a passo
# Resolver o problema
```

### Dia 3: Validação Final

```bash
# Volta para Opção 1
mvn clean test

# 18/18 testes devem passar
# Pronto para fazer commit!
```

---

## 🚨 Troubleshooting Rápido

### ❌ "Tests fail after editing code"

```bash
# Use Opção 1:
cd /Users/evandro/Projects/library/dynamodb-lib
mvn clean install -DskipTests

cd /Users/evandro/Downloads/dynamodb
mvn test
```

### ❌ "IntelliJ não vê mudanças"

```bash
# Use Opção 1:
mvn clean install -DskipTests

# Ou no IntelliJ:
Build → Rebuild Project (Cmd+Shift+K)
```

### ❌ "systemPath não funciona"

```bash
# SystemPath (Opção 2) só funciona se JAR existe
# Recompile: mvn clean package -DskipTests
# Ou use Opção 1 (maven install)
```

---

## ✨ Resumo

**Para começar:**
```bash
cd /Users/evandro/Projects/library/dynamodb-lib
mvn clean install -DskipTests

cd /Users/evandro/Downloads/dynamodb
mvn test
```

**Quando precisar debugar:**
- Abrir IntelliJ
- File → Project Structure → Modules → + Import
- Selecionar `/Users/evandro/Projects/library/dynamodb-lib`
- OK!

**Quando terminar:**
```bash
git add .
git commit -m "feat: [descrição]"
git push origin main
```

---

**Última atualização:** 2026-02-28
**Versão recomendada:** Opção 1 (Maven Install) para 90% dos casos