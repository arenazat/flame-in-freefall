-- ==============================================================================
-- Faz 4.1 — Karşılaştırmalı Grafik Alanları ve Görünüm Güncellemesi
-- Migration: 20260929000003_add_comparative_data.sql
-- ==============================================================================

-- 1. 'experiment_summaries' Tablosuna 1g vs Mikroyerçekimi Sayısal Karşılaştırma Alanlarını Ekle
ALTER TABLE public.experiment_summaries
    ADD COLUMN IF NOT EXISTS one_g_value NUMERIC(10, 4),
    ADD COLUMN IF NOT EXISTS micro_g_value NUMERIC(10, 4),
    ADD COLUMN IF NOT EXISTS comparison_unit TEXT,
    ADD COLUMN IF NOT EXISTS comparison_metric_name TEXT,
    ADD COLUMN IF NOT EXISTS comparison_notes TEXT;

-- 2. 'experiment_details_view' Görünümünü Karşılaştırmalı Alanlarla Güncelle
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
