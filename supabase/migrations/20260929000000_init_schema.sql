-- ==============================================================================
-- Flame in Freefall — Database Schema & Vector Search Initialization
-- Target: Supabase (PostgreSQL 15+ with pgvector)
-- Migration: 20260929000000_init_schema.sql
-- ==============================================================================

-- 1. Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA extensions;

-- 2. Create 'experiments' Table
-- NASA Microgravity combustion experiments (FLEX, SAFFIRE, BASS, ACME, CIR, etc.)
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

-- 3. Create 'experiment_chunks' Table
-- Embeddings storage for natural language semantic search (pgvector 1536-dim)
CREATE TABLE IF NOT EXISTS public.experiment_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    experiment_id UUID NOT NULL REFERENCES public.experiments(id) ON DELETE CASCADE,
    chunk_text TEXT NOT NULL,
    embedding extensions.vector(1536),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. Create 'experiment_summaries' Table
-- AI-generated distillation, risk assessment score, and key flammability findings
CREATE TABLE IF NOT EXISTS public.experiment_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    experiment_id UUID NOT NULL UNIQUE REFERENCES public.experiments(id) ON DELETE CASCADE,
    ai_summary TEXT NOT NULL,
    risk_score INTEGER NOT NULL CHECK (risk_score >= 0 AND risk_score <= 100),
    risk_label TEXT NOT NULL, -- 'LOW', 'MODERATE', 'HIGH', 'CRITICAL'
    key_finding TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('ok', 'insufficient_data')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. Vector Similarity Search Index (HNSW for high-recall cosine distance)
CREATE INDEX IF NOT EXISTS idx_experiment_chunks_embedding_hnsw
    ON public.experiment_chunks
    USING hnsw (embedding extensions.vector_cosine_ops);

-- 6. Query Optimization Indexes
-- Indexes for dashboard filtering and search performance
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

-- 7. Row Level Security (RLS) Configuration
-- Halka açık salt-okunur erişim: Herkes okuyabilir (anon & authenticated).
-- Yazma işlemleri yalnızca Service Role yetkisiyle yapılabilir.
ALTER TABLE public.experiments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.experiment_chunks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.experiment_summaries ENABLE ROW LEVEL SECURITY;

-- Read policies for public access
CREATE POLICY "Allow public read access on experiments"
    ON public.experiments
    FOR SELECT
    TO anon, authenticated
    USING (true);

CREATE POLICY "Allow public read access on experiment_chunks"
    ON public.experiment_chunks
    FOR SELECT
    TO anon, authenticated
    USING (true);

CREATE POLICY "Allow public read access on experiment_summaries"
    ON public.experiment_summaries
    FOR SELECT
    TO anon, authenticated
    USING (true);

-- 8. Semantic Search RPC Function
-- Natural language query match function using pgvector cosine similarity
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

-- Grant execution to public clients
GRANT EXECUTE ON FUNCTION public.match_experiment_chunks TO anon, authenticated, service_role;
