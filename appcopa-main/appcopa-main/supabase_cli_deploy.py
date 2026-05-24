import requests
import json
import os

SUPABASE_URL = "https://eoftnnsjhqqidgprpcbz.supabase.co"
SERVICE_ROLE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVvZnRubnNqaHFxaWRncHJwY2J6Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3OTU5OTYxMiwiZXhwIjoyMDk1MTc1NjEyfQ.xS_ICGa5UOh0a9hDV0wrzMG8HGRq1hzMXRXuSEPwiNc"

headers = {
    "apikey": SERVICE_ROLE_KEY,
    "Authorization": f"Bearer {SERVICE_ROLE_KEY}",
    "Content-Type": "application/json"
}

print("="*60)
print("🚀 SUPABASE DEPLOYMENT SCRIPT")
print("="*60)
print(f"\n📡 Projeto: eoftnnsjhqqidgprpcbz")
print(f"URL: {SUPABASE_URL}")

# Verificar conexão
response = requests.get(f"{SUPABASE_URL}/rest/v1/", headers=headers)
if response.status_code == 200:
    print("\n✅ Conexão com Supabase: OK")
else:
    print(f"\n❌ Erro de conexão: {response.status_code}")
    exit(1)

# Ler schema SQL
with open('supabase_schema.sql', 'r') as f:
    sql_content = f.read()

print(f"\n📄 Schema SQL carregado: {len(sql_content)} caracteres")

# Método alternativo: Criar função RPC primeiro que executa SQL dinâmico
print("\n🔧 Tentando criar função para executar SQL...")

create_function_sql = """
CREATE OR REPLACE FUNCTION execute_dynamic_sql(query_text TEXT)
RETURNS TEXT AS $$
BEGIN
    EXECUTE query_text;
    RETURN 'Success';
EXCEPTION WHEN OTHERS THEN
    RETURN SQLERRM;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
"""

# Dividir o schema em statements individuais
statements = [s.strip() for s in sql_content.split(';') if s.strip() and not s.strip().startswith('--')]

print(f"\n📋 Encontrados {len(statements)} statements SQL")

# Instruções para o usuário
print("\n" + "="*60)
print("⚠️  LIMITAÇÃO DA API REST DO SUPABASE")
print("="*60)
print("""
A API REST do Supabase não permite executar comandos DDL 
(CREATE TABLE, CREATE FUNCTION, etc.) diretamente.

✅ SOLUÇÃO RECOMENDADA (2 opções):

OPÇÃO 1 - Dashboard SQL Editor (Mais rápido - 2 minutos):
  1. Acesse: https://supabase.com/dashboard/project/eoftnnsjhqqidgprpcbz/sql/new
  2. Copie todo o conteúdo do arquivo: supabase_schema.sql
  3. Cole no editor SQL
  4. Clique em "Run" ou pressione Ctrl+Enter
  5. Aguarde a confirmação "Success"

OPÇÃO 2 - Supabase CLI (Para automação):
  1. Instale: npm install -g supabase
  2. Login: supabase login
  3. Link: supabase link --project-ref eoftnnsjhqqidgprpcbz
  4. Deploy: supabase db push --sql-file supabase_schema.sql

OPÇÃO 3 - Usar pgAdmin ou psql direto:
  Connection string: postgresql://postgres.eoftnnsjhqqidgprpcbz:[YOUR-PASSWORD]@aws-0-sa-east-1.pooler.supabase.com:6543/postgres
""")

# Gerar relatório
print("\n" + "="*60)
print("📊 RESUMO DO SCHEMA")
print("="*60)

tables = ['users', 'boloes', 'games', 'predictions', 'groups', 'group_teams', 'payments', 'analytics_events']
print("\nTabelas a serem criadas:")
for i, table in enumerate(tables, 1):
    print(f"  {i}. {table}")

print("\nRecursos incluídos:")
print("  ✓ Row Level Security (RLS) ativado")
print("  ✓ Triggers automáticos para cálculo de pontos")
print("  ✓ Índices de performance")
print("  ✓ Dados iniciais de exemplo (times da Copa 2026)")
print("  ✓ Funções automáticas de atualização")

print("\n" + "="*60)
print("🎯 PRÓXIMOS PASSOS")
print("="*60)
print("""
1. Execute o schema usando uma das opções acima
2. Após executar, teste a conexão com este script
3. O app Android já está configurado para usar o Supabase
4. As chaves Stripe já estão no .env.example

Após criar o schema, você poderá:
  ✓ Criar usuários via autenticação
  ✓ Fazer palpites nos jogos
  ✓ Processar pagamentos com Stripe
  ✓ Sincronizar dados offline/online
  ✓ Receber analytics da IA
""")

# Salvar instruções em arquivo
instructions = """
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
"""

with open('INSTRUCOES_SUPABASE.md', 'w') as f:
    f.write(instructions)

print("\n💾 Instruções salvas em: INSTRUCOES_SUPABASE.md")
print("="*60)
