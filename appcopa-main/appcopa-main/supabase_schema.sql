-- Schema SQL para Supabase
-- Executar no SQL Editor do Supabase: https://app.supabase.com/project/_/sql

-- Habilitar extensão UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabela de Usuários
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    username TEXT,
    points INTEGER DEFAULT 0,
    level_name TEXT DEFAULT 'Novato',
    xp INTEGER DEFAULT 0,
    max_xp INTEGER DEFAULT 1000,
    accurate_scores_count INTEGER DEFAULT 0,
    win_rate INTEGER DEFAULT 0,
    balance DECIMAL(10,2) DEFAULT 1000.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Tabela de Clubes
CREATE TABLE clubs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    description TEXT,
    is_private BOOLEAN DEFAULT false,
    access_password TEXT,
    entry_fee DECIMAL(10,2) DEFAULT 0.00,
    prize_first_place INTEGER DEFAULT 70,
    prize_second_place INTEGER DEFAULT 20,
    prize_third_place INTEGER DEFAULT 10,
    member_count INTEGER DEFAULT 1,
    estimated_prize_pool DECIMAL(12,2) DEFAULT 0.00,
    match_count INTEGER DEFAULT 0,
    owner_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Tabela de Membros do Clube
CREATE TABLE club_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    club_id UUID REFERENCES clubs(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    role TEXT DEFAULT 'member', -- 'owner', 'admin', 'member'
    UNIQUE(club_id, user_id)
);

-- Tabela de Partidas (Copa do Mundo)
CREATE TABLE matches (
    id INTEGER PRIMARY KEY,
    group_name TEXT NOT NULL,
    team_a TEXT NOT NULL,
    team_b TEXT NOT NULL,
    flag_a TEXT,
    flag_b TEXT,
    actual_score_a INTEGER,
    actual_score_b INTEGER,
    match_date TIMESTAMP WITH TIME ZONE,
    stadium TEXT,
    status TEXT DEFAULT 'scheduled', -- 'scheduled', 'live', 'finished'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Tabela de Palpites
CREATE TABLE predictions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    match_id INTEGER REFERENCES matches(id) ON DELETE CASCADE,
    score_a INTEGER,
    score_b INTEGER,
    points_earned DECIMAL(5,1) DEFAULT 0.0,
    exact_score_bonus BOOLEAN DEFAULT false,
    multiplier DECIMAL(3,1) DEFAULT 1.0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, match_id)
);

-- Tabela de Transações Financeiras
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    type TEXT NOT NULL, -- 'deposit', 'withdrawal', 'prize', 'fee'
    description TEXT NOT NULL,
    method TEXT NOT NULL, -- 'Stripe', 'PIX', 'System'
    amount DECIMAL(10,2) NOT NULL,
    stripe_payment_intent_id TEXT,
    status TEXT DEFAULT 'pending', -- 'pending', 'completed', 'failed', 'refunded'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Tabela de Classificação (Grupo)
CREATE TABLE group_standings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_name TEXT NOT NULL,
    team_name TEXT NOT NULL,
    flag TEXT,
    points INTEGER DEFAULT 0,
    goal_diff INTEGER DEFAULT 0,
    goals_scored INTEGER DEFAULT 0,
    played INTEGER DEFAULT 0,
    won INTEGER DEFAULT 0,
    drawn INTEGER DEFAULT 0,
    lost INTEGER DEFAULT 0,
    position INTEGER,
    qualified BOOLEAN DEFAULT false,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(group_name, team_name)
);

-- Tabela de Analytics (IA)
CREATE TABLE analytics_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    event_type TEXT NOT NULL, -- 'prediction_made', 'club_joined', 'payment_completed', etc.
    event_data JSONB,
    anomaly_detected BOOLEAN DEFAULT false,
    anomaly_type TEXT, -- 'suspicious_accuracy', 'late_prediction', etc.
    severity TEXT, -- 'low', 'medium', 'high', 'critical'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Índices para Performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_clubs_owner ON clubs(owner_id);
CREATE INDEX idx_matches_group ON matches(group_name);
CREATE INDEX idx_matches_date ON matches(match_date);
CREATE INDEX idx_matches_status ON matches(status);
CREATE INDEX idx_predictions_user ON predictions(user_id);
CREATE INDEX idx_predictions_match ON predictions(match_id);
CREATE INDEX idx_transactions_user ON transactions(user_id);
CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_standings_group ON group_standings(group_name);
CREATE INDEX idx_analytics_user ON analytics_events(user_id);
CREATE INDEX idx_analytics_type ON analytics_events(event_type);

-- Trigger para atualizar updated_at automaticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Aplicar triggers nas tabelas com updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_clubs_updated_at BEFORE UPDATE ON clubs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_matches_updated_at BEFORE UPDATE ON matches
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_predictions_updated_at BEFORE UPDATE ON predictions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_standings_updated_at BEFORE UPDATE ON group_standings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Row Level Security (RLS) - Segurança por linha
-- Habilitar RLS em todas as tabelas
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE clubs ENABLE ROW LEVEL SECURITY;
ALTER TABLE club_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE predictions ENABLE ROW LEVEL SECURITY;
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_standings ENABLE ROW LEVEL SECURITY;
ALTER TABLE analytics_events ENABLE ROW LEVEL SECURITY;

-- Policies para users
CREATE POLICY "Users can view their own data" ON users
    FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Users can update their own data" ON users
    FOR UPDATE USING (auth.uid() = id);

-- Policies para clubs (público ler, owner editar)
CREATE POLICY "Clubs are publicly viewable" ON clubs
    FOR SELECT TO authenticated USING (true);

CREATE POLICY "Owners can update their clubs" ON clubs
    FOR UPDATE USING (auth.uid() = owner_id);

CREATE POLICY "Authenticated users can create clubs" ON clubs
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = owner_id);

-- Policies para matches (apenas leitura pública)
CREATE POLICY "Matches are publicly viewable" ON matches
    FOR SELECT TO authenticated USING (true);

-- Policies para predictions (usuário vê os seus)
CREATE POLICY "Users can view their own predictions" ON predictions
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can create their own predictions" ON predictions
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own predictions" ON predictions
    FOR UPDATE USING (auth.uid() = user_id);

-- Policies para transactions (usuário vê as suas)
CREATE POLICY "Users can view their own transactions" ON transactions
    FOR SELECT USING (auth.uid() = user_id);

-- Policies para group_standings (leitura pública)
CREATE POLICY "Standings are publicly viewable" ON group_standings
    FOR SELECT TO authenticated USING (true);

-- Policies para analytics_events (apenas admin/sistema escreve)
CREATE POLICY "Users can view their own analytics" ON analytics_events
    FOR SELECT USING (auth.uid() = user_id OR user_id IS NULL);

-- Dados iniciais de exemplo (opcional)
INSERT INTO matches (id, group_name, team_a, team_b, flag_a, flag_b, match_date, stadium, status) VALUES
(1, 'A', 'BRA', 'SUI', '🇧🇷', '🇨🇭', '2026-06-24 16:00:00', 'Maracanã', 'scheduled'),
(2, 'A', 'SRB', 'CMR', '🇷🇸', '🇨🇲', '2026-06-24 16:00:00', 'Maracanã', 'scheduled'),
(3, 'A', 'BRA', 'SRB', '🇧🇷', '🇷🇸', '2026-06-28 13:00:00', 'Arena Corinthians', 'scheduled'),
(4, 'A', 'CMR', 'SUI', '🇨🇲', '🇨🇭', '2026-06-28 21:00:00', 'Mineirão', 'scheduled'),
(5, 'A', 'BRA', 'CMR', '🇧🇷', '🇨🇲', '2026-07-02 17:00:00', 'Beira-Rio', 'scheduled'),
(6, 'A', 'SUI', 'SRB', '🇨🇭', '🇷🇸', '2026-07-02 17:00:00', 'Mané Garrincha', 'scheduled');

-- Função para calcular pontos automaticamente quando resultado é atualizado
CREATE OR REPLACE FUNCTION calculate_prediction_points()
RETURNS TRIGGER AS $$
DECLARE
    pred RECORD;
    base_points INTEGER;
    multiplier DECIMAL(3,1);
    bonus_points INTEGER;
BEGIN
    -- Para cada palpite desta partida
    FOR pred IN SELECT * FROM predictions WHERE match_id = NEW.id LOOP
        -- Calcular pontos base
        IF pred.score_a IS NOT NULL AND pred.score_b IS NOT NULL THEN
            -- Verificar se acertou o resultado
            IF (pred.score_a > pred.score_b AND NEW.actual_score_a > NEW.actual_score_b) OR
               (pred.score_a < pred.score_b AND NEW.actual_score_a < NEW.actual_score_b) OR
               (pred.score_a = pred.score_b AND NEW.actual_score_a = NEW.actual_score_b) THEN
                -- Acertou vencedor/empate
                IF NEW.actual_score_a = NEW.actual_score_b THEN
                    base_points := 1; -- Empate vale 1 ponto
                ELSE
                    base_points := 3; -- Vencedor vale 3 pontos
                END IF;
                
                -- Verificar placar exato
                IF pred.score_a = NEW.actual_score_a AND pred.score_b = NEW.actual_score_b THEN
                    multiplier := 2.0;
                    bonus_points := 5;
                ELSE
                    multiplier := 1.0;
                    bonus_points := 0;
                END IF;
                
                -- Atualizar palpite com pontos
                UPDATE predictions 
                SET points_earned = (base_points * multiplier) + bonus_points,
                    exact_score_bonus = (bonus_points > 0),
                    multiplier = multiplier,
                    updated_at = NOW()
                WHERE id = pred.id;
                
                -- Atualizar pontos totais do usuário
                UPDATE users 
                SET points = points + ((base_points * multiplier) + bonus_points),
                    updated_at = NOW()
                WHERE id = pred.user_id;
            END IF;
        END IF;
    END LOOP;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger para calcular pontos quando partida é atualizada
CREATE TRIGGER trigger_calculate_prediction_points
    AFTER UPDATE ON matches
    FOR EACH ROW
    WHEN (OLD.actual_score_a IS DISTINCT FROM NEW.actual_score_a OR 
          OLD.actual_score_b IS DISTINCT FROM NEW.actual_score_b)
    EXECUTE FUNCTION calculate_prediction_points();
