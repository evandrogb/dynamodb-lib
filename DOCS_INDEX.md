# 📚 Índice de Documentação

Guia rápido para encontrar a documentação que você precisa.

## 🎯 Escolha Seu Caso de Uso

### 🚀 "Quero começar a desenvolver AGORA"

1. Leia: **[TESTING_GUIDE.md](TESTING_GUIDE.md)** (5 min)
2. Execute:
   ```bash
   cd /Users/evandro/Projects/library/dynamodb-lib
   mvn clean install -DskipTests

   cd /Users/evandro/Downloads/dynamodb
   mvn test
   ```
3. Pronto! 18/18 testes passam

---

### 🐛 "Preciso debugar um bug na lib"

1. Leia: **[TESTING_GUIDE.md](TESTING_GUIDE.md)** → Opção 3 (IntelliJ)
2. Abra IntelliJ:
   ```bash
   open -a "IntelliJ IDEA" /Users/evandro/Downloads/dynamodb
   ```
3. File → Project Structure → Modules → + Import
4. Selecione a lib e OK
5. Coloque breakpoint e debug!

---

### 📖 "Quero entender toda a arquitetura"

Leia nesta ordem:
1. **[README.md](README.md)** - Visão geral da lib
2. **[DEVELOPMENT.md](DEVELOPMENT.md)** - Setup e workflow
3. **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - Como testar

---

### 🤝 "Quero contribuir com features"

1. Leia: **[CONTRIBUTING.md](CONTRIBUTING.md)**
2. Follow:
   - Conventional commits
   - Testes devem passar (18/18)
   - Estilos de código
3. Abra um Pull Request!

---

### 🎯 "Vou publicar uma nova versão no JitPack"

1. Leia: **[DEVELOPMENT.md](DEVELOPMENT.md)** → "Atualizar JitPack"
2. Execute:
   ```bash
   git add .
   git commit -m "feat: [descrição]"
   git tag v1.0.1
   git push origin main && git push origin v1.0.1
   ```
3. Ativar em: https://jitpack.io/#evandrogb/dynamodb-lib

---

### ❌ "Estou com erro/problema"

1. Procure em: **[TESTING_GUIDE.md](TESTING_GUIDE.md)** → Troubleshooting
2. Se não resolver, abra uma [Issue](https://github.com/evandrogb/dynamodb-lib/issues)

---

## 📋 Todos os Documentos

### Na Biblioteca

| Arquivo | Para Quê | Tempo |
|---------|----------|-------|
| **README.md** | Como usar a lib em outros projetos | 10 min |
| **DEVELOPMENT.md** | Setup local, 3 opções de teste, JitPack | 15 min |
| **TESTING_GUIDE.md** | Visual/prático, compare as 3 opções | 10 min |
| **CONTRIBUTING.md** | Como contribuir, commits, PRs | 10 min |
| **DOCS_INDEX.md** | Este arquivo, guia de navegação | 5 min |

### No Projeto Principal

| Arquivo | Para Quê | Tempo |
|---------|----------|-------|
| **JITPACK_TESTING.md** | Como testar com JitPack remoto | 10 min |
| **TEST_JITPACK.sh** | Script automático para testar | 1 min |
| **LIBRARY_EXTRACTION_SUMMARY.md** | Resumo técnico da extração | 10 min |

---

## 🔥 Atalhos Rápidos

### Setup Inicial
```bash
git clone https://github.com/evandrogb/dynamodb-lib.git
cd dynamodb-lib
mvn clean install -DskipTests
```

### Testar Localmente
```bash
cd /Users/evandro/Projects/library/dynamodb-lib
mvn clean install -DskipTests

cd /Users/evandro/Downloads/dynamodb
mvn test
```

### Publicar Nova Versão
```bash
git add .
git commit -m "feat: [descrição]"
git tag v1.0.1
git push origin main && git push origin v1.0.1
# Depois ativar em: https://jitpack.io/#evandrogb/dynamodb-lib
```

### Debug no IntelliJ
```bash
open -a "IntelliJ IDEA" /Users/evandro/Downloads/dynamodb
# File → Project Structure → Modules → + Import
# Selecione: /Users/evandro/Projects/library/dynamodb-lib
```

---

## 🎓 Aprendizado Recomendado

### Novo na Biblioteca?

```
README.md (5 min)
    ↓
TESTING_GUIDE.md (10 min)
    ↓
Executar: mvn clean install
    ↓
DEVELOPMENT.md (ler conforme necessário)
```

### Fazendo Mudanças?

```
TESTING_GUIDE.md → Opção 1 ou 3
    ↓
Editar código
    ↓
Testar: mvn test
    ↓
CONTRIBUTING.md (antes de fazer PR)
```

### Publicando?

```
DEVELOPMENT.md → "Atualizar JitPack"
    ↓
git tag v1.0.1
    ↓
git push origin tags/v1.0.1
    ↓
Ativar em JitPack
```

---

## 📊 Mapa Mental

```
┌─ README.md (Use a lib?)
├─ DEVELOPMENT.md (Setup?)
├─ TESTING_GUIDE.md (Como testar?)
│  ├─ Opção 1: Maven Install
│  ├─ Opção 2: SystemPath
│  └─ Opção 3: IntelliJ Debug
├─ CONTRIBUTING.md (Contribuir?)
└─ DOCS_INDEX.md (Este arquivo)
```

---

## 🔗 Links Importantes

- **Repositório:** https://github.com/evandrogb/dynamodb-lib
- **JitPack:** https://jitpack.io/#evandrogb/dynamodb-lib
- **Issues:** https://github.com/evandrogb/dynamodb-lib/issues
- **Pull Requests:** https://github.com/evandrogb/dynamodb-lib/pulls

---

## ✨ Dicas Finais

1. **Comece simples:** Use Opção 1 (Maven Install) para 90% dos casos
2. **Recompile frequente:** `mvn clean install -DskipTests` é seu melhor amigo
3. **Teste sempre:** `mvn test` deve passar 18/18 antes de fazer commit
4. **Conventions:** Use conventional commits para manter histórico limpo
5. **Documentação:** Atualize README/docs quando adicionar features

---

## 📞 Precisa de Ajuda?

- Procure em **TESTING_GUIDE.md** → Troubleshooting
- Leia **DEVELOPMENT.md** → Comandos úteis
- Abra uma [Issue](https://github.com/evandrogb/dynamodb-lib/issues)

---

**Última atualização:** 2026-02-28
**Status:** Documentação Completa ✅