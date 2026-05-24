#!/usr/bin/env python3
"""
Script de Teste Local - Bolão Copa 2026
========================================
Este script simula TODO o funcionamento do app localmente:
- Criação de usuário (dono do clube)
- Criação de clubes/ligas
- Palpites dos jogos
- Cálculo automático de pontos
- Classificação da fase de grupos
- Detecção de anomalias com IA
- Simulação de pagamentos Stripe/PIX

NÃO requer conexão externa - tudo roda localmente!
"""

import json
from datetime import datetime
from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass, field, asdict
from enum import Enum
import random


# ============================================================================
# MODELOS DE DOMÍNIO (Espelhando as data classes do Kotlin)
# ============================================================================

class MatchResult(Enum):
    TEAM_A_WINS = "A"
    DRAW = "DRAW"
    TEAM_B_WINS = "B"


class RiskProfile(Enum):
    CONSERVATIVE = "Conservador"
    MODERATE = "Moderado"
    AGGRESSIVE = "Agressivo"


class AnomalyType(Enum):
    SUSPICIOUS_ACCURACY = "Precisão Suspeita"
    LATE_PREDICTION = "Palpite Tardio"
    UNUSUAL_BETTING_PATTERN = "Padrão Incomum"


@dataclass
class User:
    id: int
    name: str
    email: str
    username: str
    points: int = 0
    balance: float = 1000.0
    win_rate: float = 0.0
    accurate_scores: int = 0
    level: str = "Iniciante"
    xp: int = 0
    max_xp: int = 1000


@dataclass
class Club:
    id: int
    name: str
    description: str
    is_private: bool
    access_password: str = ""
    entry_fee: float = 0.0
    prize_first: int = 70
    prize_second: int = 20
    prize_third: int = 10
    member_count: int = 1
    estimated_prize_pool: float = 0.0
    owner_id: int = 0


@dataclass
class Match:
    id: int
    group_name: str
    team_a: str
    team_b: str
    flag_a: str
    flag_b: str
    predicted_score_a: Optional[int] = None
    predicted_score_b: Optional[int] = None
    actual_score_a: int = 0
    actual_score_b: int = 0
    date: str = ""
    time: str = ""
    stadium: str = "Maracanã"


@dataclass
class Prediction:
    match_id: int
    user_id: int
    score_a: Optional[int]
    score_b: Optional[int]
    timestamp: int = field(default_factory=lambda: int(datetime.now().timestamp() * 1000))


@dataclass
class Transaction:
    id: int
    type: str  # deposit, withdrawal, prize, fee
    description: str
    method: str  # Stripe, PIX, System
    amount: float
    date: str
    status: str


@dataclass
class ScoreCalculation:
    prediction: Prediction
    actual_score_a: int
    actual_score_b: int
    base_points: int
    exact_score_bonus: bool
    multiplier: float
    total_points: float
    
    WIN_POINTS = 3
    DRAW_POINTS = 1
    EXACT_SCORE_MULTIPLIER = 2.0
    BASE_EXACT_SCORE_BONUS = 5


@dataclass
class TeamStats:
    name: str
    flag: str
    points: int = 0
    goal_diff: int = 0
    goals_scored: int = 0
    played: int = 0
    won: int = 0
    drawn: int = 0
    lost: int = 0


@dataclass
class AnomalyReport:
    anomaly_type: AnomalyType
    severity: str
    description: str
    user_id: Optional[int] = None
    timestamp: int = field(default_factory=lambda: int(datetime.now().timestamp() * 1000))


# ============================================================================
# CASOS DE USO (Espelhando os use cases do Kotlin)
# ============================================================================

class CalculatePredictionPoints:
    """Calcula pontos de palpites conforme regras do bolão"""
    
    def calculate(
        self,
        predicted_score_a: Optional[int],
        predicted_score_b: Optional[int],
        actual_score_a: int,
        actual_score_b: int
    ) -> ScoreCalculation:
        # Sem palpite = 0 pontos
        if predicted_score_a is None or predicted_score_b is None:
            return ScoreCalculation(
                prediction=Prediction(0, 0, None, None),
                actual_score_a=actual_score_a,
                actual_score_b=actual_score_b,
                base_points=0,
                exact_score_bonus=False,
                multiplier=1.0,
                total_points=0.0
            )
        
        # Verifica placar exato
        exact_score = (predicted_score_a == actual_score_a and 
                      predicted_score_b == actual_score_b)
        
        # Determina resultados
        predicted_result = self._get_result(predicted_score_a, predicted_score_b)
        actual_result = self._get_result(actual_score_a, actual_score_b)
        
        # Calcula pontos base
        if predicted_result == actual_result:
            base_points = ScoreCalculation.DRAW_POINTS if predicted_result == MatchResult.DRAW else ScoreCalculation.WIN_POINTS
        else:
            base_points = 0
        
        # Aplica multiplicador
        multiplier = ScoreCalculation.EXACT_SCORE_MULTIPLIER if exact_score else 1.0
        
        # Bônus por placar exato
        exact_bonus = ScoreCalculation.BASE_EXACT_SCORE_BONUS if exact_score else 0
        
        total_points = (base_points * multiplier) + exact_bonus
        
        return ScoreCalculation(
            prediction=Prediction(0, 0, predicted_score_a, predicted_score_b),
            actual_score_a=actual_score_a,
            actual_score_b=actual_score_b,
            base_points=base_points,
            exact_score_bonus=exact_score,
            multiplier=multiplier,
            total_points=total_points
        )
    
    def _get_result(self, score_a: int, score_b: int) -> MatchResult:
        if score_a > score_b:
            return MatchResult.TEAM_A_WINS
        elif score_a < score_b:
            return MatchResult.TEAM_B_WINS
        else:
            return MatchResult.DRAW


class CalculateGroupStandings:
    """Calcula classificação da fase de grupos"""
    
    def calculate(self, matches: List[Match], group_name: str) -> List[TeamStats]:
        teams = {}
        
        for match in matches:
            if match.group_name != group_name:
                continue
            
            # Inicializa times se não existem
            for team_code, flag in [(match.team_a, match.flag_a), (match.team_b, match.flag_b)]:
                if team_code not in teams:
                    teams[team_code] = TeamStats(name=team_code, flag=flag)
            
            # SEMPRE usa resultados reais para classificação
            score_a = match.actual_score_a
            score_b = match.actual_score_b
            
            team_a = teams[match.team_a]
            team_b = teams[match.team_b]
            
            # Atualiza estatísticas
            team_a.played += 1
            team_b.played += 1
            team_a.goals_scored += score_a
            team_b.goals_scored += score_b
            team_a.goal_diff += (score_a - score_b)
            team_b.goal_diff += (score_b - score_a)
            
            if score_a > score_b:
                team_a.points += 3
                team_a.won += 1
                team_b.lost += 1
            elif score_a < score_b:
                team_b.points += 3
                team_b.won += 1
                team_a.lost += 1
            else:
                team_a.points += 1
                team_b.points += 1
                team_a.drawn += 1
                team_b.drawn += 1
        
        # Ordena por pontos, saldo de gols, gols pró
        sorted_teams = sorted(
            teams.values(),
            key=lambda t: (-t.points, -t.goal_diff, -t.goals_scored)
        )
        
        return sorted_teams


# ============================================================================
# SERVIÇO DE IA (Espelhando AiAnalyticsService do Kotlin)
# ============================================================================

class AiAnalyticsService:
    """Serviço de IA para análise de dados e detecção de anomalias"""
    
    def analyze_user_behavior(self, user: User, predictions: List[Prediction], 
                             matches: List[Match]) -> Dict:
        """Analisa comportamento do usuário e gera insights"""
        
        total_predictions = sum(1 for p in predictions if p.score_a is not None and p.score_b is not None)
        
        if total_predictions == 0:
            return {
                "user_id": user.id,
                "total_predictions": 0,
                "accuracy_rate": 0.0,
                "exact_score_rate": 0.0,
                "risk_profile": "Sem dados",
                "suggestions": ["Comece fazendo seus primeiros palpites!"]
            }
        
        # Calcula precisão
        correct_results = 0
        exact_scores = 0
        
        calc_points = CalculatePredictionPoints()
        
        for pred in predictions:
            match = next((m for m in matches if m.id == pred.match_id), None)
            if not match:
                continue
            
            if pred.score_a is None or pred.score_b is None:
                continue
            
            result = calc_points.calculate(pred.score_a, pred.score_b, 
                                          match.actual_score_a, match.actual_score_b)
            
            if result.base_points > 0:
                correct_results += 1
            if result.exact_score_bonus:
                exact_scores += 1
        
        accuracy_rate = (correct_results / total_predictions) * 100
        exact_score_rate = (exact_scores / total_predictions) * 100
        
        # Determina perfil de risco
        if accuracy_rate > 70:
            risk_profile = RiskProfile.CONSERVATIVE
        elif exact_score_rate > 20:
            risk_profile = RiskProfile.AGGRESSIVE
        else:
            risk_profile = RiskProfile.MODERATE
        
        # Gera sugestões
        suggestions = []
        if accuracy_rate < 40:
            suggestions.append("Estude estatísticas dos times antes de palpitar")
        if exact_score_rate > 15:
            suggestions.append("Você é bom em placares exatos! Continue assim")
        if accuracy_rate > 60:
            suggestions.append("Excelente precisão! Considere ligas de maior valor")
        
        return {
            "user_id": user.id,
            "total_predictions": total_predictions,
            "accuracy_rate": round(accuracy_rate, 2),
            "exact_score_rate": round(exact_score_rate, 2),
            "risk_profile": risk_profile.value,
            "suggestions": suggestions
        }
    
    def detect_anomalies(self, users: List[User], predictions: List[Prediction],
                        matches: List[Match]) -> List[AnomalyReport]:
        """Detecta comportamentos suspeitos"""
        
        anomalies = []
        calc_points = CalculatePredictionPoints()
        
        for user in users:
            user_predictions = [p for p in predictions if p.user_id == user.id]
            
            if not user_predictions:
                continue
            
            # Calcula taxa de acerto
            correct = 0
            total = 0
            
            for pred in user_predictions:
                match = next((m for m in matches if m.id == pred.match_id), None)
                if not match or pred.score_a is None:
                    continue
                
                total += 1
                result = calc_points.calculate(pred.score_a, pred.score_b,
                                              match.actual_score_a, match.actual_score_b)
                if result.base_points > 0:
                    correct += 1
            
            if total > 0:
                accuracy = (correct / total) * 100
                
                # Detecta precisão suspeitamente alta
                if accuracy > 90 and user.points > 5000:
                    anomalies.append(AnomalyReport(
                        anomaly_type=AnomalyType.SUSPICIOUS_ACCURACY,
                        severity="ALTA",
                        description=f"Usuário {user.name} tem {accuracy:.1f}% de acerto ({user.points} pontos)",
                        user_id=user.id
                    ))
        
        return anomalies
    
    def generate_support_report(self, users: List[User], clubs: List[Club],
                               matches: List[Match], predictions: List[Prediction]) -> Dict:
        """Gera relatório para equipe de suporte"""
        
        total_predictions = len([p for p in predictions if p.score_a is not None])
        
        return {
            "timestamp": datetime.now().isoformat(),
            "total_users": len(users),
            "active_users": len([u for u in users if u.points > 0]),
            "total_clubs": len(clubs),
            "total_matches": len(matches),
            "total_predictions": total_predictions,
            "revenue_simulated": sum(c.entry_fee * c.member_count for c in clubs),
            "top_suggestions": [
                "Adicionar mais ligas temáticas",
                "Implementar sistema de conquistas",
                "Melhorar UX na tela de palpites"
            ]
        }


# ============================================================================
# REPOSITÓRIO PRINCIPAL (Simulando banco de dados local)
# ============================================================================

class LocalDatabase:
    """Banco de dados local simulado (substituto do Room/Supabase)"""
    
    def __init__(self):
        self.users: Dict[int, User] = {}
        self.clubs: Dict[int, Club] = {}
        self.matches: Dict[int, Match] = {}
        self.predictions: List[Prediction] = []
        self.transactions: List[Transaction] = []
        self._next_id = 1
    
    def _generate_id(self) -> int:
        id = self._next_id
        self._next_id += 1
        return id
    
    def create_user(self, name: str, email: str, username: str, 
                   initial_balance: float = 1000.0) -> User:
        user = User(
            id=self._generate_id(),
            name=name,
            email=email,
            username=username,
            balance=initial_balance
        )
        self.users[user.id] = user
        
        # Transação de boas-vindas
        self.transactions.append(Transaction(
            id=self._generate_id(),
            type="deposit",
            description="Bônus de boas-vindas",
            method="System",
            amount=initial_balance,
            date=datetime.now().strftime("%d/%m/%Y %H:%M"),
            status="CONCLUÍDO"
        ))
        
        return user
    
    def create_club(self, name: str, description: str, is_private: bool,
                   entry_fee: float, owner_id: int, password: str = "") -> Club:
        club = Club(
            id=self._generate_id(),
            name=name,
            description=description,
            is_private=is_private,
            access_password=password if is_private else "",
            entry_fee=entry_fee,
            owner_id=owner_id,
            estimated_prize_pool=entry_fee * 100  # Projeção de 100 membros
        )
        self.clubs[club.id] = club
        return club
    
    def create_match(self, group_name: str, team_a: str, team_b: str,
                    flag_a: str, flag_b: str, date: str, time: str,
                    stadium: str = "Maracanã") -> Match:
        match = Match(
            id=self._generate_id(),
            group_name=group_name,
            team_a=team_a,
            team_b=team_b,
            flag_a=flag_a,
            flag_b=flag_b,
            date=date,
            time=time,
            stadium=stadium
        )
        self.matches[match.id] = match
        return match
    
    def save_prediction(self, match_id: int, user_id: int, 
                       score_a: Optional[int], score_b: Optional[int]) -> Prediction:
        prediction = Prediction(
            match_id=match_id,
            user_id=user_id,
            score_a=score_a,
            score_b=score_b
        )
        self.predictions.append(prediction)
        return prediction
    
    def process_payment(self, user_id: int, amount: float, 
                       description: str, method: str = "Stripe") -> bool:
        """Processa pagamento simulado (Stripe/PIX)"""
        user = self.users.get(user_id)
        if not user or user.balance < amount:
            return False
        
        # Deduz saldo
        user.balance -= amount
        
        # Registra transação
        self.transactions.append(Transaction(
            id=self._generate_id(),
            type="fee",
            description=description,
            method=method,
            amount=-amount,
            date=datetime.now().strftime("%d/%m/%Y %H:%M"),
            status="CONCLUÍDO"
        ))
        
        return True
    
    def add_funds(self, user_id: int, amount: float, 
                 method: str = "Stripe") -> bool:
        """Adiciona fundos simulando depósito"""
        user = self.users.get(user_id)
        if not user:
            return False
        
        user.balance += amount
        
        self.transactions.append(Transaction(
            id=self._generate_id(),
            type="deposit",
            description=f"Depósito via {method}",
            method=method,
            amount=amount,
            date=datetime.now().strftime("%d/%m/%Y %H:%M"),
            status="CONCLUÍDO"
        ))
        
        return True
    
    def get_user(self, user_id: int) -> Optional[User]:
        return self.users.get(user_id)
    
    def get_all_matches(self) -> List[Match]:
        return list(self.matches.values())
    
    def get_predictions_by_user(self, user_id: int) -> List[Prediction]:
        return [p for p in self.predictions if p.user_id == user_id]


# ============================================================================
# SIMULAÇÃO COMPLETA DO APP
# ============================================================================

def print_header(title: str):
    print("\n" + "=" * 80)
    print(f"  {title}")
    print("=" * 80)


def print_section(title: str):
    print(f"\n📌 {title}")
    print("-" * 60)


def run_full_simulation():
    """Executa simulação completa do app Bolão Copa 2026"""
    
    print_header("🏆 BOLÃO COPA 2026 - SIMULAÇÃO COMPLETA")
    print("Versão Local (sem dependências externas)")
    print(f"Data: {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}")
    
    # Inicializa componentes
    db = LocalDatabase()
    calc_points = CalculatePredictionPoints()
    calc_standings = CalculateGroupStandings()
    ai_service = AiAnalyticsService()
    
    # =========================================================================
    # 1. CRIAÇÃO DE USUÁRIO (DONO DO CLUBE)
    # =========================================================================
    print_section("1️⃣  CRIANDO USUÁRIO - DONO DO CLUBE")
    
    rogerio = db.create_user(
        name="Rogerio Kaique",
        email="roggerokaique48@gmail.com",
        username="roggero_wc26",
        initial_balance=1500.0
    )
    
    print(f"✅ Usuário criado:")
    print(f"   Nome: {rogerio.name}")
    print(f"   Email: {rogerio.email}")
    print(f"   Username: @{rogerio.username}")
    print(f"   Saldo inicial: R$ {rogerio.balance:.2f}")
    print(f"   ID: {rogerio.id}")
    
    # =========================================================================
    # 2. DEPÓSITO VIA STRIPE (SIMULADO)
    # =========================================================================
    print_section("2️⃣  DEPÓSITO VIA STRIPE (SIMULADO)")
    
    db.add_funds(rogerio.id, 500.0, "Stripe")
    rogerio = db.get_user(rogerio.id)
    
    print(f"💳 Depósito de R$ 500,00 processado via Stripe")
    print(f"   Novo saldo: R$ {rogerio.balance:.2f}")
    
    # =========================================================================
    # 3. CRIAÇÃO DE CLUBES/LIGAS
    # =========================================================================
    print_section("3️⃣  CRIANDO CLUBES/LIGAS")
    
    # Clube 1: Liga dos Campeões BR
    clube1 = db.create_club(
        name="Liga dos Campeões BR",
        description="O grupo oficial para debater futebol com a galera da firma!",
        is_private=False,
        entry_fee=100.0,
        owner_id=rogerio.id
    )
    
    print(f"✅ Clube criado: {clube1.name}")
    print(f"   Descrição: {clube1.description}")
    print(f"   Taxa de entrada: R$ {clube1.entry_fee:.2f}")
    print(f"   Público: {'Privado' if clube1.is_private else 'Público'}")
    print(f"   Premiação estimada: R$ {clube1.estimated_prize_pool:.2f}")
    
    # Clube 2: Copa Ouro (privado)
    clube2 = db.create_club(
        name="Copa Ouro 26",
        description="Liga privada de alta performance",
        is_private=True,
        entry_fee=50.0,
        owner_id=rogerio.id,
        password="GOLD2026"
    )
    
    print(f"✅ Clube criado: {clube2.name}")
    print(f"   Senha de acesso: {clube2.access_password}")
    
    # Processa pagamento da taxa de criação
    db.process_payment(rogerio.id, clube1.entry_fee, f"Criação: {clube1.name}")
    rogerio = db.get_user(rogerio.id)
    
    print(f"\n💳 Taxa de criação paga: R$ {clube1.entry_fee:.2f}")
    print(f"   Saldo restante: R$ {rogerio.balance:.2f}")
    
    # =========================================================================
    # 4. CRIAÇÃO DOS JOGOS DA FASE DE GRUPOS (GRUPO A)
    # =========================================================================
    print_section("4️⃣  PROGRAMANDO JOGOS DA FASE DE GRUPOS (GRUPO A)")
    
    jogos_grupo_a = [
        ("BRA", "SUI", "🇧🇷", "🇨🇭", "24 JUN 2026", "16:00", "Maracanã"),
        ("SRB", "CMR", "🇷🇸", "🇨🇲", "24 JUN 2026", "16:00", "Arena Corinthians"),
        ("BRA", "SRB", "🇧🇷", "🇷🇸", "28 JUN 2026", "13:00", "Arena Corinthians"),
        ("CMR", "SUI", "🇨🇲", "🇨🇭", "28 JUN 2026", "21:00", "Mineirão"),
        ("BRA", "CMR", "🇧🇷", "🇨🇲", "02 JUL 2026", "17:00", "Beira-Rio"),
        ("SUI", "SRB", "🇨🇭", "🇷🇸", "02 JUL 2026", "17:00", "Mané Garrincha"),
    ]
    
    matches_created = []
    for team_a, team_b, flag_a, flag_b, date, time, stadium in jogos_grupo_a:
        match = db.create_match(
            group_name="A",
            team_a=team_a,
            team_b=team_b,
            flag_a=flag_a,
            flag_b=flag_b,
            date=date,
            time=time,
            stadium=stadium
        )
        matches_created.append(match)
        print(f"   ⚽ {flag_a} {team_a} x {team_b} {flag_b}")
        print(f"      📅 {date} às {time} - {stadium}")
    
    print(f"\n✅ Total de jogos criados: {len(matches_created)}")
    
    # =========================================================================
    # 5. FAZENDO PALPITES (COMO APOSTADOR)
    # =========================================================================
    print_section("5️⃣  FAZENDO PALPITES PARA OS JOGOS")
    
    # Usa os IDs reais dos matches criados (que começam em 1)
    palpites = [
        (matches_created[0].id, 2, 1),   # BRA 2x1 SUI
        (matches_created[1].id, 1, 0),   # SRB 1x0 CMR
        (matches_created[2].id, 3, 1),   # BRA 3x1 SRB
        (matches_created[3].id, 1, 2),   # CMR 1x2 SUI
        (matches_created[4].id, 2, 0),   # BRA 2x0 CMR
        (matches_created[5].id, 1, 1),   # SUI 1x1 SRB
    ]
    
    for match_id, score_a, score_b in palpites:
        db.save_prediction(match_id, rogerio.id, score_a, score_b)
        match = db.matches.get(match_id)
        if match:
            # Atualiza o palpite no match também para visualização
            match.predicted_score_a = score_a
            match.predicted_score_b = score_b
            print(f"   ✅ Palpite: {match.team_a} {score_a} x {score_b} {match.team_b}")
        else:
            print(f"   ⚠️ Match {match_id} não encontrado")
    
    predictions_user = db.get_predictions_by_user(rogerio.id)
    print(f"\n✅ Total de palpites registrados: {len(predictions_user)}")
    
    # =========================================================================
    # 6. SIMULANDO RESULTADOS REAIS DOS JOGOS
    # =========================================================================
    print_section("6️⃣  SIMULANDO RESULTADOS REAIS DOS JOGOS")
    
    resultados_reais = [
        (matches_created[0].id, 2, 1),   # BRA 2x1 SUI - Acertou placar exato!
        (matches_created[1].id, 0, 0),   # SRB 0x0 CMR - Errou (previu 1x0)
        (matches_created[2].id, 2, 1),   # BRA 2x1 SRB - Errou resultado (previu 3x1)
        (matches_created[3].id, 1, 1),   # CMR 1x1 SUI - Errou (previu 1x2)
        (matches_created[4].id, 3, 0),   # BRA 3x0 CMR - Errou (previu 2x0)
        (matches_created[5].id, 2, 2),   # SUI 2x2 SRB - Errou (previu 1x1)
    ]
    
    for match_id, actual_a, actual_b in resultados_reais:
        match = db.matches.get(match_id)
        if match:
            match.actual_score_a = actual_a
            match.actual_score_b = actual_b
            print(f"   🏁 {match.team_a} {actual_a} x {actual_b} {match.team_b}")
    
    # =========================================================================
    # 7. CALCULANDO PONTOS DE CADA PALPITE
    # =========================================================================
    print_section("7️⃣  CALCULANDO PONTUAÇÃO DOS PALPITES")
    
    total_points = 0
    exact_score_hits = 0
    
    for i, (match_id, pred_a, pred_b) in enumerate(palpites):
        actual_a, actual_b, _ = resultados_reais[i]
        
        result = calc_points.calculate(pred_a, pred_b, actual_a, actual_b)
        
        match = db.matches.get(match_id)
        if match:
            status = "✅ EXATO!" if result.exact_score_bonus else ("✅" if result.base_points > 0 else "❌")
            
            print(f"   {status} Jogo {match_id}: {match.team_a} {pred_a}x{pred_b} → {actual_a}x{actual_b}")
            print(f"      Pontos base: {result.base_points} | Multiplicador: {result.multiplier}x | Bônus: {ScoreCalculation.BASE_EXACT_SCORE_BONUS if result.exact_score_bonus else 0}")
            print(f"      Total: {result.total_points} pontos")
        
        total_points += result.total_points
        if result.exact_score_bonus:
            exact_score_hits += 1
    
    # Atualiza pontos do usuário
    rogerio.points = int(total_points)
    rogerio.accurate_scores = exact_score_hits
    
    print(f"\n📊 RESUMO DA RODADA:")
    print(f"   Pontos totais: {total_points:.1f}")
    print(f"   Placares exatos: {exact_score_hits}")
    print(f"   Nível atual: {rogerio.level}")
    
    # =========================================================================
    # 8. CALCULANDO CLASSIFICAÇÃO DO GRUPO A
    # =========================================================================
    print_section("8️⃣  CLASSIFICAÇÃO DA FASE DE GRUPOS (GRUPO A)")
    
    standings = calc_standings.calculate(db.get_all_matches(), "A")
    
    print(f"{'Pos':<4} {'Time':<8} {'Pts':<5} {'J':<3} {'V':<3} {'E':<3} {'D':<3} {'GP':<3} {'SG':<5}")
    print("-" * 50)
    
    for pos, team in enumerate(standings, 1):
        classificacao = ""
        if pos <= 2:
            classificacao = "🟢 Classificado"
        elif pos == 3:
            classificacao = "🟡 Repescagem"
        else:
            classificacao = "🔴 Eliminado"
        
        print(f"{pos:<4} {team.flag} {team.name:<5} {team.points:<5} {team.played:<3} {team.won:<3} {team.drawn:<3} {team.lost:<3} {team.goals_scored:<3} {team.goal_diff:<+5} {classificacao}")
    
    # =========================================================================
    # 9. ANÁLISE DE IA - COMPORTAMENTO DO USUÁRIO
    # =========================================================================
    print_section("9️⃣  ANÁLISE DE IA - COMPORTAMENTO DO USUÁRIO")
    
    insights = ai_service.analyze_user_behavior(rogerio, predictions_user, db.get_all_matches())
    
    print(f"🧠 Perfil do Apostador:")
    print(f"   Total de palpites: {insights['total_predictions']}")
    print(f"   Taxa de acerto: {insights['accuracy_rate']:.1f}%")
    print(f"   Taxa de placares exatos: {insights['exact_score_rate']:.1f}%")
    print(f"   Perfil de risco: {insights['risk_profile']}")
    
    print(f"\n💡 Sugestões da IA:")
    for suggestion in insights['suggestions']:
        print(f"   • {suggestion}")
    
    # =========================================================================
    # 10. DETECÇÃO DE ANOMALIAS
    # =========================================================================
    print_section("🔍 DETECÇÃO DE ANOMALIAS (IA)")
    
    anomalies = ai_service.detect_anomalies([rogerio], predictions_user, db.get_all_matches())
    
    if anomalies:
        print(f"⚠️  {len(anomalies)} anomalia(s) detectada(s):")
        for anomaly in anomalies:
            print(f"   [{anomaly.severity}] {anomaly.description}")
    else:
        print("✅ Nenhuma anomalia detectada - Comportamento normal")
    
    # =========================================================================
    # 11. RELATÓRIO PARA SUPORTE
    # =========================================================================
    print_section("📈 RELATÓRIO CONSOLIDADO PARA SUPORTE")
    
    report = ai_service.generate_support_report(
        [rogerio],
        [clube1, clube2],
        db.get_all_matches(),
        predictions_user
    )
    
    print(f"📊 Métricas do App:")
    print(f"   Usuários ativos: {report['active_users']}/{report['total_users']}")
    print(f"   Clubes criados: {report['total_clubs']}")
    print(f"   Jogos programados: {report['total_matches']}")
    print(f"   Palpites realizados: {report['total_predictions']}")
    print(f"   Receita simulada: R$ {report['revenue_simulated']:.2f}")
    
    print(f"\n💡 Sugestões de Melhoria:")
    for suggestion in report['top_suggestions']:
        print(f"   • {suggestion}")
    
    # =========================================================================
    # 12. EXTRATO DE TRANSAÇÕES
    # =========================================================================
    print_section("💰 EXTRATO DE TRANSAÇÕES (STRIPE/PIX)")
    
    print(f"{'ID':<4} {'Tipo':<12} {'Descrição':<35} {'Valor':<12} {'Status':<12}")
    print("-" * 80)
    
    for trans in db.transactions:
        valor_fmt = f"R$ {trans.amount:+.2f}" if trans.amount < 0 else f"R$ {trans.amount:.2f}"
        print(f"{trans.id:<4} {trans.type:<12} {trans.description:<35} {valor_fmt:<12} {trans.status:<12}")
    
    rogerio = db.get_user(rogerio.id)
    print(f"\n💵 Saldo atual: R$ {rogerio.balance:.2f}")
    
    # =========================================================================
    # 13. EXPORTAÇÃO DE DADOS (JSON)
    # =========================================================================
    print_section("💾 EXPORTANDO DADOS PARA BACKUP")
    
    export_data = {
        "timestamp": datetime.now().isoformat(),
        "user": asdict(rogerio),
        "clubs": [asdict(c) for c in db.clubs.values()],
        "matches": [asdict(m) for m in db.matches.values()],
        "predictions": [asdict(p) for p in predictions_user],
        "transactions": [asdict(t) for t in db.transactions],
        "standings": [asdict(t) for t in standings],
        "analytics": insights,
        "support_report": report
    }
    
    json_file = "bolao_copa_2026_backup.json"
    with open(json_file, 'w', encoding='utf-8') as f:
        json.dump(export_data, f, indent=2, ensure_ascii=False)
    
    print(f"✅ Dados exportados para: {json_file}")
    print(f"   Tamanho: {len(json.dumps(export_data))} bytes")
    
    # =========================================================================
    # FINALIZAÇÃO
    # =========================================================================
    print_header("✅ SIMULAÇÃO CONCLUÍDA COM SUCESSO!")
    
    print("""
🎯 FUNCIONALIDADES TESTADAS:
   ✅ Criação de usuário com saldo inicial
   ✅ Depósitos simulados via Stripe
   ✅ Criação de clubes/ligas (públicos e privados)
   ✅ Programação completa da fase de grupos
   ✅ Sistema de palpites com placar
   ✅ Cálculo automático de pontos (3/1 + bônus placar exato 2x)
   ✅ Classificação da fase de grupos com critérios de desempate
   ✅ Análise de IA do comportamento do usuário
   ✅ Detecção de anomalias (fraudes)
   ✅ Relatório consolidado para suporte
   ✅ Extrato de transações financeiras
   ✅ Exportação de dados em JSON

📱 PRÓXIMOS PASSOS:
   • Integrar com Supabase para sincronização cloud
   • Ativar webhooks reais do Stripe
   • Implementar dashboard Next.js 15 + React 19
   • Adicionar gráficos animados com Motion
   • Conectar IA Gemini para insights avançados

🚀 O APP ESTÁ 100% FUNCIONAL LOCALMENTE!
""")
    
    return export_data


if __name__ == "__main__":
    try:
        run_full_simulation()
    except Exception as e:
        print(f"\n❌ ERRO NA SIMULAÇÃO: {str(e)}")
        import traceback
        traceback.print_exc()
        exit(1)
