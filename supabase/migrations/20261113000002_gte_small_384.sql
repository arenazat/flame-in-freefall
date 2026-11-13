-- ==============================================================================
-- Flame in Freefall — Embedding modelini Supabase gte-small'a (384 boyut) geçir
-- Migration: 20261113000002_gte_small_384.sql
-- Not: Mevcut vektör parçaları (farklı modelle üretilmiş) silinir; ingestion `embed` komutu yeniden üretir.
-- ==============================================================================

-- 1. Eski vektörleri temizle (boyut değişeceği için)
DELETE FROM public.experiment_chunks;

-- 2. Eski indeks ve fonksiyonları kaldır (imza vector(1536) içeriyor)
DROP INDEX IF EXISTS public.idx_experiment_chunks_embedding_hnsw;
DROP FUNCTION IF EXISTS public.match_experiment_chunks(extensions.vector, double precision, integer);
DROP FUNCTION IF EXISTS public.match_experiment_chunks_with_metadata(extensions.vector, double precision, integer);

-- 3. Kolonu 384 boyuta çevir
ALTER TABLE public.experiment_chunks
    ALTER COLUMN embedding TYPE extensions.vector(384);

-- 4. HNSW indeksini yeniden oluştur
CREATE INDEX IF NOT EXISTS idx_experiment_chunks_embedding_hnsw
    ON public.experiment_chunks
    USING hnsw (embedding extensions.vector_cosine_ops);

-- 5. Semantik arama RPC'leri (384 boyut)
CREATE OR REPLACE FUNCTION public.match_experiment_chunks(
    query_embedding extensions.vector(384),
    match_threshold FLOAT DEFAULT 0.3,
    match_count INT DEFAULT 10
)
RETURNS TABLE (id UUID, experiment_id UUID, chunk_text TEXT, similarity FLOAT)
LANGUAGE plpgsql STABLE AS $$
BEGIN
    RETURN QUERY
    SELECT ec.id, ec.experiment_id, ec.chunk_text,
           (1 - (ec.embedding <=> query_embedding))::FLOAT AS similarity
    FROM public.experiment_chunks ec
    WHERE ec.embedding IS NOT NULL
      AND (1 - (ec.embedding <=> query_embedding)) > match_threshold
    ORDER BY ec.embedding <=> query_embedding
    LIMIT match_count;
END;
$$;

GRANT EXECUTE ON FUNCTION public.match_experiment_chunks TO anon, authenticated, service_role;

CREATE OR REPLACE FUNCTION public.match_experiment_chunks_with_metadata(
    query_embedding extensions.vector(384),
    match_threshold FLOAT DEFAULT 0.25,
    match_count INT DEFAULT 8
)
RETURNS TABLE (
    chunk_id UUID, experiment_id UUID, experiment_title TEXT, experiment_series TEXT,
    experiment_source_url TEXT, chunk_text TEXT, similarity FLOAT
)
LANGUAGE plpgsql STABLE AS $$
BEGIN
    RETURN QUERY
    SELECT ec.id, e.id, e.title, e.series, e.source_url, ec.chunk_text,
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
