import requests
import json

# Configurações
SUPABASE_URL = "https://eoftnnsjhqqidgprpcbz.supabase.co"
SERVICE_ROLE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVvZnRubnNqaHFxaWRncHJwY2J6Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3OTU5OTYxMiwiZXhwIjoyMDk1MTc1NjEyfQ.xS_ICGa5UOh0a9hDV0wrzMG8HGRq1hzMXRXuSEPwiNc"

headers = {
    "apikey": SERVICE_ROLE_KEY,
    "Authorization": f"Bearer {SERVICE_ROLE_KEY}",
    "Content-Type": "application/json",
    "Prefer": "return=representation"
}

# Ler o schema SQL
with open('supabase_schema.sql', 'r') as f:
    sql_content = f.read()

print("📡 Conectando ao Supabase...")
print(f"URL: {SUPABASE_URL}")

# Testar conexão simples primeiro
try:
    response = requests.get(f"{SUPABASE_URL}/rest/v1/", headers=headers)
    print(f"\n✅ Conexão básica: Status {response.status_code}")
    
    # Agora executar o schema SQL via API RPC
    print("\n📝 Executando schema SQL...")
    
    rpc_payload = {
        "query": sql_content
    }
    
    # Usar endpoint de query direta (disponível com service_role)
    response = requests.post(
        f"{SUPABASE_URL}/rest/v1/rpc/exec_sql",
        headers=headers,
        json={"query": sql_content}
    )
    
    if response.status_code in [200, 201, 204]:
        print("✅ Schema executado com sucesso!")
        print(f"Resposta: {response.status_code}")
    else:
        print(f"⚠️ Status da resposta: {response.status_code}")
        print(f"Conteúdo: {response.text[:500]}")
        
except Exception as e:
    print(f"❌ Erro: {str(e)}")
