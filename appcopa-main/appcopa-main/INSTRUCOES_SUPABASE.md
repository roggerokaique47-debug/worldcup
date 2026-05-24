
# Como Executar o Schema no Supabase

## Opção 1: Dashboard (Recomendado)

1. Acesse: https://supabase.com/dashboard/project/eoftnnsjhqqidgprpcbz/sql/new
2. Abra o arquivo `supabase_schema.sql` neste diretório
3. Copie TODO o conteúdo
4. Cole no editor SQL do dashboard
5. Clique em "Run" (ou Ctrl+Enter)
6. Aguarde a mensagem de sucesso
7. Pronto! Seu banco está configurado.

## Opção 2: Supabase CLI

```bash
# Instalar
npm install -g supabase

# Login
supabase login

# Link ao projeto
supabase link --project-ref eoftnnsjhqqidgprpcbz

# Deploy do schema
supabase db push --sql-file supabase_schema.sql
```

## Verificação

Após executar, você pode verificar no dashboard:
- Aba "Table Editor": deve mostrar 8 tabelas
- Aba "SQL Editor": execute `SELECT * FROM users;` para testar
