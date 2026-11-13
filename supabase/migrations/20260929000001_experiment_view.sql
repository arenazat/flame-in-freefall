-- ==============================================================================
-- Migration: 20260929000001_experiment_view.sql
-- Flame in Freefall — Joined Experiments & Summaries View
-- ==============================================================================

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
    s.status AS summary_status
FROM public.experiments e
LEFT JOIN public.experiment_summaries s ON e.id = s.experiment_id;

-- Grant select permission to public (anon) and authenticated roles
GRANT SELECT ON public.experiment_details_view TO anon, authenticated, service_role;
