# 🧪 Teste de Conexão Supabase - Bolão Copa 2026

Este documento guia você para testar a conexão com o Supabase.

## Pré-requisitos

1. **Conta no Supabase**: https://supabase.com (plano gratuito disponível)
2. **Projeto criado**: Anote a URL e chaves API
3. **Schema aplicado**: Execute o SQL no editor do Supabase

## Passo a Passo

### 1. Criar Projeto no Supabase

```
1. Acesse https://supabase.com
2. Clique em "New Project"
3. Preencha:
   - Name: bolao-copa-2026
   - Database Password: (guarde bem!)
   - Region: escolha a mais próxima (us-east-1 para Brasil)
4. Aguarde ~2 minutos para criação
```

### 2. Obter Credenciais

No dashboard do Supabase:
```
Settings > API

Você verá:
- Project URL: https://xxxxxxxxxxxxx.supabase.co
- API Keys:
  - anon/public (para o app móvel)
  - service_role (para backend/admin)
```

### 3. Aplicar Schema SQL

```
1. No dashboard, vá para SQL Editor
2. Copie TODO o conteúdo de `supabase_schema.sql`
3. Cole no editor e clique "Run"
4. Verifique se todas as 8 tabelas foram criadas
```

### 4. Configurar .env no Projeto Android

Crie/editar `.env` na raiz do projeto:

```env
SUPABASE_URL=https://xxxxxxxxxxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
SUPABASE_SERVICE_ROLE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 5. Executar Teste de Conexão

#### Opção A: Teste Unitário
```bash
cd /workspace/appcopa-main/appcopa-main
./gradlew testDebugUnitTest --tests "*SupabaseDatabaseTest*"
```

#### Opção B: Teste Manual no App

Adicione este código temporário na `MainActivity.kt`:

```kotlin
// Na onCreate ou em um botão de teste
lifecycleScope.launch {
    val result = SupabaseDatabase.testConnection(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY
    )
    
    if (result.success) {
        println("✅ ${result.message}")
        println("📋 ${result.details}")
    } else {
        println("❌ ${result.message}")
        println("🔍 ${result.details}")
    }
}
```

### 6. Interpretar Resultados

#### ✅ Sucesso
```
Conexão bem-sucedida! Supabase configurado corretamente.
URL: https://xxx.supabase.co
Status: 200
```

#### ❌ Erros Comuns

| Erro | Causa | Solução |
|------|-------|---------|
| `401 Unauthorized` | Chave ANON inválida | Copie novamente de Settings > API |
| `404 Not Found` | URL incorreta | Verifique se copiou a URL completa |
| `relation "users" does not exist` | Schema não aplicado | Execute o SQL no editor |
| `timeout` | Sem internet | Verifique conexão/emulador |
| `network error` | Firewall/proxy | Teste em outra rede |

## Validação do Schema

Após aplicar o SQL, verifique no Table Editor se existem:

- [ ] users
- [ ] clubs
- [ ] club_members
- [ ] matches
- [ ] predictions
- [ ] payments
- [ ] analytics_events
- [ ] ai_suggestions

## Próximos Passos

1. **Autenticação**: Testar login/cadastro de usuários
2. **CRUD**: Criar/ler clubes e palpites
3. **Sincronização**: Implementar sync local ↔ cloud
4. **Webhooks**: Configurar Stripe + Supabase Functions

## Troubleshooting

### "Could not connect to Supabase"
- Verifique se a URL começa com `https://`
- Confirme que não há espaços em branco na chave
- Teste a URL no navegador: `https://xxx.supabase.co/rest/v1/users`

### "Permission denied"
- Ative Row Level Security (RLS) nas tabelas
- Configure policies para `anon` role
- Verifique em Authentication > Policies

### Dados não aparecem
- Execute: `SELECT * FROM users;` no SQL Editor
- Verifique se há dados: `SELECT COUNT(*) FROM matches;`

## Integração com MCP (Model Context Protocol)

Para usar MCP do Supabase:

```typescript
// Exemplo de configuração MCP
{
  "mcpServers": {
    "supabase": {
      "command": "npx",
      "args": ["-y", "@supabase/mcp-server"],
      "env": {
        "SUPABASE_URL": "https://xxx.supabase.co",
        "SUPABASE_SERVICE_ROLE_KEY": "your-key"
      }
    }
  }
}
```

## Links Úteis

- [Docs Supabase](https://supabase.com/docs)
- [SDK Kotlin](https://github.com/jan-gould/supabase-kt)
- [Stripe + Supabase](https://supabase.com/docs/guides/getting-started/quickstarts/stripe)
- [RLS Policies](https://supabase.com/docs/guides/auth/row-level-security)

---

**Status**: ✅ Pronto para testes
**Última atualização**: 2026
**Versão**: 1.0
