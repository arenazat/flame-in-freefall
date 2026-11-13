-- ==============================================================================
-- Flame in Freefall — Tam Supabase Veritabanı ve Şema Kurulum Scripti
-- Target: Supabase (PostgreSQL 15+ with pgvector)
-- Dosya: setup_all.sql
-- ==============================================================================

-- 1. pgvector Uzantısını Etkinleştir
CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA extensions;

-- 2. 'experiments' Tablosunu Oluştur
-- NASA Mikroyerçekimi yanma deneyleri (FLEX, SAFFIRE, BASS, ACME, CIR vb.)
CREATE TABLE IF NOT EXISTS public.experiments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    series TEXT NOT NULL,
    material TEXT NOT NULL,
    gravity_level TEXT NOT NULL,
    oxygen_concentration NUMERIC(5, 2),
    year INTEGER,
    source_url TEXT NOT NULL, -- ZORUNLU: Her kaydın NASA PSI veya NTRS kaynağına izlenebilirliği
    source_type TEXT NOT NULL CHECK (source_type IN ('psi', 'ntrs')),
    raw_text TEXT,
    needs_manual_review BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. 'experiment_chunks' Tablosunu Oluştur
-- Semantik arama için metin parçaları ve 1536 boyutlu vektör embeddingleri
CREATE TABLE IF NOT EXISTS public.experiment_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    experiment_id UUID NOT NULL REFERENCES public.experiments(id) ON DELETE CASCADE,
    chunk_text TEXT NOT NULL,
    embedding extensions.vector(1536),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. 'experiment_summaries' Tablosunu Oluştur
-- AI tarafından üretilen özet, risk skoru (0-100), risk etiketi ve güvenlik bulgusu
CREATE TABLE IF NOT EXISTS public.experiment_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    experiment_id UUID NOT NULL UNIQUE REFERENCES public.experiments(id) ON DELETE CASCADE,
    ai_summary TEXT NOT NULL,
    risk_score INTEGER NOT NULL CHECK (risk_score >= 0 AND risk_score <= 100),
    risk_label TEXT NOT NULL, -- 'LOW', 'MODERATE', 'HIGH', 'CRITICAL'
    key_finding TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('ok', 'insufficient_data')),
    one_g_value NUMERIC(10, 4), -- Faz 4.1: 1g (Dünya) ölçüm değeri (ör. alev yayılma hızı)
    micro_g_value NUMERIC(10, 4), -- Faz 4.1: Mikroyerçekimi (ug) ölçüm değeri
    comparison_unit TEXT, -- Faz 4.1: Birim (ör. mm/s, cm/s)
    comparison_metric_name TEXT, -- Faz 4.1: Metrik adı (ör. Alev Yayılma Hızı)
    comparison_notes TEXT, -- Faz 4.1: Karşılaştırmalı bulgu açıklaması
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. HNSW Vektör Benzerlik İndeksi (Kosinüs Mesafesi)
CREATE INDEX IF NOT EXISTS idx_experiment_chunks_embedding_hnsw
    ON public.experiment_chunks
    USING hnsw (embedding extensions.vector_cosine_ops);

-- 6. Sorgu ve Filtreleme İndeksleri
CREATE INDEX IF NOT EXISTS idx_experiments_series ON public.experiments(series);
CREATE INDEX IF NOT EXISTS idx_experiments_material ON public.experiments(material);
CREATE INDEX IF NOT EXISTS idx_experiments_gravity_level ON public.experiments(gravity_level);
CREATE INDEX IF NOT EXISTS idx_experiments_oxygen_concentration ON public.experiments(oxygen_concentration);
CREATE INDEX IF NOT EXISTS idx_experiments_year ON public.experiments(year);
CREATE INDEX IF NOT EXISTS idx_experiments_source_type ON public.experiments(source_type);
CREATE INDEX IF NOT EXISTS idx_experiments_filters ON public.experiments(series, gravity_level, year);

CREATE INDEX IF NOT EXISTS idx_experiment_chunks_experiment_id ON public.experiment_chunks(experiment_id);

CREATE INDEX IF NOT EXISTS idx_experiment_summaries_experiment_id ON public.experiment_summaries(experiment_id);
CREATE INDEX IF NOT EXISTS idx_experiment_summaries_risk_score ON public.experiment_summaries(risk_score);
CREATE INDEX IF NOT EXISTS idx_experiment_summaries_status ON public.experiment_summaries(status);

-- 7. Satır Düzeyi Güvenlik (RLS)
ALTER TABLE public.experiments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.experiment_chunks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.experiment_summaries ENABLE ROW LEVEL SECURITY;

-- Okuma Politikaları (Anonim ve Yetkilendirilmiş Kullanıcılar için Halka Açık Salt-Okunur)
DROP POLICY IF EXISTS "Allow public read access on experiments" ON public.experiments;
CREATE POLICY "Allow public read access on experiments"
    ON public.experiments
    FOR SELECT
    TO anon, authenticated
    USING (true);

DROP POLICY IF EXISTS "Allow public read access on experiment_chunks" ON public.experiment_chunks;
CREATE POLICY "Allow public read access on experiment_chunks"
    ON public.experiment_chunks
    FOR SELECT
    TO anon, authenticated
    USING (true);

DROP POLICY IF EXISTS "Allow public read access on experiment_summaries" ON public.experiment_summaries;
CREATE POLICY "Allow public read access on experiment_summaries"
    ON public.experiment_summaries
    FOR SELECT
    TO anon, authenticated
    USING (true);

-- 8. Semantik Arama RPC Fonksiyonu (match_experiment_chunks)
CREATE OR REPLACE FUNCTION public.match_experiment_chunks(
    query_embedding extensions.vector(1536),
    match_threshold FLOAT DEFAULT 0.3,
    match_count INT DEFAULT 10
)
RETURNS TABLE (
    id UUID,
    experiment_id UUID,
    chunk_text TEXT,
    similarity FLOAT
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT
        ec.id,
        ec.experiment_id,
        ec.chunk_text,
        (1 - (ec.embedding <=> query_embedding))::FLOAT AS similarity
    FROM public.experiment_chunks ec
    WHERE (1 - (ec.embedding <=> query_embedding)) > match_threshold
    ORDER BY ec.embedding <=> query_embedding
    LIMIT match_count;
END;
$$;

GRANT EXECUTE ON FUNCTION public.match_experiment_chunks TO anon, authenticated, service_role;

-- 9. Deneyler ve Özetleri Birleştiren Görünüm (experiment_details_view)
CREATE OR REPLACE VIEW public.experiment_details_view
WITH (security_invoker = true) AS
SELECT
    e.id,
    e.title,
    e.series,
    e.material,
    e.gravity_level,
    e.oxygen_concentration,
    e.year,
    e.source_url,
    e.source_type,
    e.raw_text,
    e.needs_manual_review,
    e.created_at,
    s.id AS summary_id,
    s.ai_summary,
    s.risk_score,
    s.risk_label,
    s.key_finding,
    s.status AS summary_status,
    s.one_g_value,
    s.micro_g_value,
    s.comparison_unit,
    s.comparison_metric_name,
    s.comparison_notes
FROM public.experiments e
LEFT JOIN public.experiment_summaries s ON e.id = s.experiment_id;

GRANT SELECT ON public.experiment_details_view TO anon, authenticated, service_role;

-- 10. Deney Üst Verileriyle Zenginleştirilmiş Semantik Arama RPC'si
CREATE OR REPLACE FUNCTION public.match_experiment_chunks_with_metadata(
    query_embedding extensions.vector(1536),
    match_threshold FLOAT DEFAULT 0.25,
    match_count INT DEFAULT 8
)
RETURNS TABLE (
    chunk_id UUID,
    experiment_id UUID,
    experiment_title TEXT,
    experiment_series TEXT,
    experiment_source_url TEXT,
    chunk_text TEXT,
    similarity FLOAT
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT
        ec.id AS chunk_id,
        e.id AS experiment_id,
        e.title AS experiment_title,
        e.series AS experiment_series,
        e.source_url AS experiment_source_url,
        ec.chunk_text,
        (1 - (ec.embedding <=> query_embedding))::FLOAT AS similarity
    FROM public.experiment_chunks ec
    JOIN public.experiments e ON ec.experiment_id = e.id
    WHERE ec.embedding IS NOT NULL
      AND (1 - (ec.embedding <=> query_embedding)) > match_threshold
    ORDER BY ec.embedding <=> query_embedding
    LIMIT match_count;
END;
$$;

GRANT EXECUTE ON FUNCTION public.match_experiment_chunks_with_metadata TO anon, authenticated, service_role;
