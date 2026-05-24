import requests
import json

SUPABASE_URL = "https://eoftnnsjhqqidgprpcbz.supabase.co"
SERVICE_ROLE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVvZnRubnNqaHFxaWRncHJwY2J6Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3OTU5OTYxMiwiZXhwIjoyMDk1MTc1NjEyfQ.xS_ICGa5UOh0a9hDV0wrzMG8HGRq1hzMXRXuSEPwiNc"

headers = {
    "apikey": SERVICE_ROLE_KEY,
    "Authorization": f"Bearer {SERVICE_ROLE_KEY}",
    "Content-Type": "application/json"
}

print("📡 Testando conexão com Supabase...")
print(f"URL: {SUPABASE_URL}")

# Test 1: Conexão básica
response = requests.get(f"{SUPABASE_URL}/rest/v1/", headers=headers)
print(f"\n1️⃣ Conexão básica: Status {response.status_code}")

# Test 2: Tentar criar uma tabela simples primeiro
print("\n2️⃣ Criando tabela de teste...")
create_table_sql = """
CREATE TABLE IF NOT EXISTS test_connection (
    id SERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    message TEXT
);
"""

# Supabase não permite executar SQL arbitrário via REST
# Precisamos usar a API de migrações ou dashboard
print("\n⚠️  A API REST do Supabase não permite executar DDL (CREATE TABLE) diretamente.")
print("✅ Solução: Use o Dashboard SQL Editor ou CLI do Supabase.")
print(f"\n📋 Instruções:")
print(f"   1. Acesse: {SUPABASE_URL.replace('.supabase.co', '.supabase.co/dashboard/project/eoftnnsjhqqidgprpcbz/sql/new')}")
print(f"   2. Cole o conteúdo do arquivo supabase_schema.sql")
print(f"   3. Clique em 'Run'")

# Test 3: Verificar se já existe alguma tabela
print("\n3️⃣ Verificando tabelas existentes...")
response = requests.get(
    f"{SUPABASE_URL}/rest/v1/?limit=1",
    headers=headers
)
print(f"Status: {response.status_code}")

# Test 4: Listar schemas
print("\n4️⃣ Listando schemas...")
response = requests.get(
    f"{SUPABASE_URL}/rest/v1/information_schema/schemata",
    headers=headers
)
print(f"Status: {response.status_code}")
if response.status_code == 200:
    print(f"Resposta: {response.json()[:200] if len(response.text) > 200 else response.json()}")
