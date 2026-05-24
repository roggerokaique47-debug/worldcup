<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# ⚽ Bolão Copa 2026 - World Cup Betting App

Aplicativo Android completo para bolão da Copa do Mundo 2026 com:
- 🎯 Sistema de palpites com pontuação inteligente
- 💳 Pagamentos via Stripe e PIX
- ☁️ Sincronização com Supabase (opcional)
- 🤖 IA para análise de padrões e detecção de anomalias
- 📊 Gráficos animados e dashboard administrativo
- 🏆 Clubes/ligas privadas e públicas

[![Android](https://img.shields.io/badge/Android-24+-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Compose-1.5+-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 🚀 Funcionalidades

### Fase de Grupos
- ✅ Todos os jogos da fase de grupos pré-configurados
- ✅ Palpites com placar exato
- ✅ Sistema de pontos: 3 pts (vitória), 1 pt (empate)
- ✅ Multiplicador 2x + 5 pts bônus para placar exato
- ✅ Classificação automática por grupo (pontos, saldo de gols, gols pró)
- ✅ Ponto extra por acertar classificados (1º e 2º lugar)

### Pagamentos
- 💳 Integração Stripe para cartões de crédito
- 🇧🇷 PIX via Stripe Payments
- 💰 Carteira virtual no app
- 📝 Histórico de transações
- 🔒 Ambiente seguro (test e production)

### Inteligência Artificial
- 🧠 Análise de padrões de apostas
- 🚨 Detecção de anomalias (fraudes)
- 📈 Perfis de usuário (conservador, moderado, agressivo)
- 💡 Sugestões personalizadas
- 📊 Dashboard para suporte técnico

### UI/UX
- 🎨 Material Design 3
- 🌙 Dark mode nativo
- ✨ Animações Lottie
- 📱 Jetpack Compose
- ♿ Acessibilidade

---

## 📦 Instalação e Configuração

### Pré-requisitos
- Android Studio Hedgehog ou superior
- JDK 17+
- Emulador Android API 24+ ou dispositivo físico
- Conta no [Stripe](https://stripe.com) (para pagamentos)
- Conta no [Supabase](https://supabase.com) (opcional, para cloud)

### Passo a Passo

1. **Clone o repositório**
```bash
git clone https://github.com/seu-usuario/appcopa.git
cd appcopa-main/appcopa-main
```

2. **Configure as variáveis de ambiente**
```bash
# Copie o arquivo de exemplo
cp .env.example .env

# Edite com suas chaves
nano .env
```

3. **Preencha o `.env` com suas chaves:**
```env
# Gemini AI (opcional)
GEMINI_API_KEY=sua-chave-aqui

# Stripe (obrigatório para pagamentos)
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_SECRET_KEY=sk_test_...

# Supabase (opcional para cloud)
SUPABASE_URL=https://seu-projeto.supabase.co
SUPABASE_ANON_KEY=sua-chave-anon
SUPABASE_SERVICE_ROLE_KEY=sua-chave-service

# Ambiente
APP_ENVIRONMENT=development
```

4. **Configure o banco Supabase (opcional)**
```bash
# Acesse https://app.supabase.com
# Crie novo projeto
# Vá em SQL Editor e execute o arquivo:
# supabase_schema.sql
```

5. **Abra no Android Studio**
- File > Open > Selecione a pasta `appcopa-main/appcopa-main`
- Aguarde o Gradle sync
- Se houver erros de incompatibilidade, clique em "Fix"

6. **Remova a configuração de signing (desenvolvimento)**
No arquivo `app/build.gradle.kts`, remova ou comente:
```kotlin
signingConfig = signingConfigs.getByName("debugConfig")
```

7. **Execute o app**
- Selecione um emulador ou dispositivo conectado
- Clique em Run (Shift+F10)

---

## 🧪 Testes Unitários

O projeto inclui testes abrangentes:

```bash
# Rodar todos os testes
./gradlew test

# Testes específicos
./gradlew test --tests CalculatePredictionPointsTest
./gradlew test --tests CalculateGroupStandingsTest
./gradlew test --tests AiAnalyticsServiceTest
./gradlew test --tests MainRepositoryTest
```

### Cobertura de Testes
- **CalculatePredictionPointsTest**: 11 testes
  - Vitória correta, derrota, empate
  - Placar exato com multiplicador
  - Palpites nulos ou parciais
  
- **CalculateGroupStandingsTest**: 10 testes
  - Ordenação por pontos
  - Critérios de desempate
  - Times classificados
  
- **AiAnalyticsServiceTest**: 6 testes
  - Detecção de anomalias
  - Perfis de risco
  - Geração de sugestões
  
- **MainRepositoryTest**: 10 testes
  - Entidades (User, Club, Match, Transaction)
  - Operações de cópia e atualização

---

## 🏗️ Arquitetura

```
app/
├── src/main/java/com/example/
│   ├── data/                  # Camada de dados
│   │   ├── AppDatabase.kt     # Room Database local
│   │   ├── Entities.kt        # Entidades Room
│   │   ├── Daos.kt            # Data Access Objects
│   │   ├── MainRepository.kt  # Repositório principal
│   │   └── SupabaseDatabase.kt# Integração cloud (opcional)
│   │
│   ├── domain/                # Regras de negócio
│   │   ├── model/             # Modelos de domínio
│   │   └── usecase/           # Casos de uso
│   │       ├── CalculatePredictionPoints.kt
│   │       └── CalculateGroupStandings.kt
│   │
│   ├── services/              # Serviços externos
│   │   ├── PaymentService.kt  # Interface de pagamento
│   │   ├── StripePaymentService.kt
│   │   └── AiAnalyticsService.kt
│   │
│   └── ui/                    # Interface do usuário
│       ├── screens/           # Telas Compose
│       ├── theme/             # Tema Material 3
│       └── MainViewModel.kt   # ViewModel principal
│
├── src/test/                  # Testes unitários locais
└── src/androidTest/           # Testes instrumentados
```

---

## 💳 Configurar Stripe para Pagamentos Reais

### Modo Teste (Desenvolvimento)
1. Acesse https://dashboard.stripe.com/test/apikeys
2. Copie as chaves de teste
3. Cole no `.env`:
```env
STRIPE_PUBLISHABLE_KEY=pk_test_51H...
STRIPE_SECRET_KEY=sk_test_51H...
```

### Modo Produção
1. Ative o modo production no Stripe
2. Use as chaves live (começam com `pk_live_` e `sk_live_`)
3. Configure webhooks em https://dashboard.stripe.com/webhooks
4. Endpoint: `https://seu-servidor.com/stripe-webhook`

### PIX com Stripe
O Stripe suporta PIX automaticamente:
```kotlin
// No StripePaymentService.kt
val paymentIntentParams = PaymentIntentParams.createCreatePaymentIntentParams(
    clientSecret = clientSecret,
    paymentMethodId = paymentMethodId
)
// O Stripe detectará automaticamente se é PIX baseado no país
```

---

## ☁️ Integração Supabase

### Vantagens
- ✅ Sincronização cloud em tempo real
- ✅ Autenticação segura (email/senha, Google, etc.)
- ✅ Banco PostgreSQL robusto
- ✅ Row Level Security (RLS)
- ✅ Plano gratuito generoso (500MB, 50k usuários/mês)

### Configuração
1. Crie conta em https://supabase.com
2. Novo projeto > Escolha região (us-east-1 recomendado)
3. SQL Editor > Execute `supabase_schema.sql`
4. Settings > API > Copie as chaves
5. Cole no `.env`

### Sincronização Automática
O app sincroniza quando há internet:
- Upload de palpites locais
- Download de resultados atualizados
- Resolução de conflitos por timestamp

---

## 🤖 IA e Analytics

O `AiAnalyticsService` fornece:

### Para Usuários
- Perfil de risco (Conservador/Moderado/Agressivo)
- Taxa de acerto e histórico
- Sugestões personalizadas
- Tendência de otimismo/pessimismo

### Para Admins/Suporte
- Detecção de fraudes (>90% acerto = suspeito)
- Atividades incomuns (palpites tardios)
- Métricas de crescimento
- Feedback automático de melhorias

### Integração com Gemini AI (Opcional)
```kotlin
// Em AiAnalyticsService.kt
suspend fun getMatchPrediction(teamA: String, teamB: String): String {
    val prompt = "Analise $teamA vs $teamB e preveja o resultado"
    // Chamar API Gemini
    return prediction
}
```

---

## 📊 Roadmap

### ✅ Implementado (v1.0)
- [x] Sistema de palpites e pontuação
- [x] Banco de dados local (Room)
- [x] Interface Jetpack Compose
- [x] Serviços de pagamento (simulado)
- [x] IA de analytics
- [x] Testes unitários
- [x] Schema Supabase pronto

### 🔄 Em Andamento (v1.1)
- [ ] Integração real Stripe
- [ ] Sincronização Supabase ativa
- [ ] Webhooks de pagamento
- [ ] Notificações push (FCM)

### 📅 Planejado (v2.0)
- [ ] IA preditiva com Gemini
- [ ] Gráficos animados no dashboard
- [ ] Sistema de conquistas
- [ ] Compartilhamento social
- [ ] Chat entre membros
- [ ] Transmissão de jogos

---

## 🛡️ Segurança

- ✅ Room com SQL sanitizado
- ✅ Chaves de API no `.env` (não versionado)
- ✅ ProGuard/R8 configurado
- ✅ HTTPS obrigatório
- ✅ Row Level Security no Supabase
- ✅ Validação de inputs

**Importante**: Nunca commitar o arquivo `.env`!

---

## 📝 Licença

MIT License - ver [LICENSE](LICENSE) para detalhes.

---

## 👥 Contribuição

Contribuições são bem-vindas! Veja como:

1. Fork o projeto
2. Crie uma branch (`git checkout -b feature/AmazingFeature`)
3. Commit (`git commit -m 'Add AmazingFeature'`)
4. Push (`git push origin feature/AmazingFeature`)
5. Pull Request

---

## 📞 Suporte

- **Documentação completa**: Ver `MELHORIAS.md`
- **Schema do banco**: `supabase_schema.sql`
- **Issues**: https://github.com/seu-usuario/appcopa/issues
- **Email**: suporte@bolaoapp.com

---

## 🙏 Agradecimentos

- [Android Developers](https://developer.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Stripe](https://stripe.com/)
- [Supabase](https://supabase.com/)
- [Google Gemini AI](https://ai.google.dev/)

---

<div align="center">

**Feito com ❤️ para a Copa do Mundo 2026**

[⬆ Voltar ao topo](#-bolão-copa-2026---world-cup-betting-app)

</div>
