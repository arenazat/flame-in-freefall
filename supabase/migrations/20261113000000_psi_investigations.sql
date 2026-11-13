-- ==============================================================================
-- Flame in Freefall - NASA PSI investigation schema (NASA challenge alignment)
-- Migration: 20261113000000_psi_investigations.sql
-- Adds: PSI investigations, test conditions (experimental tables), publications,
--       relations, habitat profiles, fire incidents, Earth applications,
--       habitat ranking + comparison helpers. Existing tables are kept.
-- ==============================================================================

-- 1. PSI investigations (one row per NASA PSI combustion investigation)
CREATE TABLE IF NOT EXISTS public.psi_investigations (
    psi_id TEXT PRIMARY KEY,                       -- e.g. PSI-69
    acronym TEXT,                                  -- e.g. FLEX
    title TEXT NOT NULL,
    project_type TEXT,                             -- Microgravity Investigation / Ground Investigation
    sub_research_area TEXT,
    platform TEXT,                                 -- ISS, Cygnus...
    hardware TEXT,
    hardware_group TEXT,                           -- ACME / CIR, MDCA / CIR, MSG flow duct (SPICE), Saffire / Cygnus...
    start_date DATE,
    end_date DATE,
    doi TEXT,
    data_size_bytes BIGINT,
    nasa_center TEXT,
    objective TEXT,
    approach TEXT,
    hypothesis TEXT,
    research_impacts TEXT,
    safety_category TEXT CHECK (safety_category IN ('prevention','detection','suppression','fundamental','modeling')),
    fuel_state TEXT CHECK (fuel_state IN ('solid','liquid_droplet','gas','aerosol')),
    fuels TEXT[] NOT NULL DEFAULT '{}',
    diluents TEXT[] NOT NULL DEFAULT '{}',
    sample_geometry TEXT,
    o2_min NUMERIC(5,2),                           -- ambient/oxidizer O2, % by volume
    o2_max NUMERIC(5,2),
    pressure_min_atm NUMERIC(6,3),
    pressure_max_atm NUMERIC(6,3),
    flow_min_cms NUMERIC(6,2),                     -- imposed air flow, cm/s (0 = quiescent)
    flow_max_cms NUMERIC(6,2),
    sensors TEXT[] NOT NULL DEFAULT '{}',
    relevance_spacecraft SMALLINT CHECK (relevance_spacecraft BETWEEN 0 AND 10),
    relevance_station SMALLINT CHECK (relevance_station BETWEEN 0 AND 10),
    relevance_lunar_base SMALLINT CHECK (relevance_lunar_base BETWEEN 0 AND 10),
    relevance_mars_base SMALLINT CHECK (relevance_mars_base BETWEEN 0 AND 10),
    relevance_rover SMALLINT CHECK (relevance_rover BETWEEN 0 AND 10),
    relevance_note TEXT,
    earth_applications TEXT[] NOT NULL DEFAULT '{}',
    source_url TEXT NOT NULL,
    test_count INTEGER NOT NULL DEFAULT 0,
    publication_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Test conditions = rows of each investigation's PSI "Experimental table" CSV (kept verbatim as JSON)
CREATE TABLE IF NOT EXISTS public.psi_test_conditions (
    id BIGSERIAL PRIMARY KEY,
    psi_id TEXT NOT NULL REFERENCES public.psi_investigations(psi_id) ON DELETE CASCADE,
    source_file TEXT NOT NULL,
    row_index INTEGER NOT NULL,
    data JSONB NOT NULL
);

-- 3. Publications
CREATE TABLE IF NOT EXISTS public.psi_publications (
    id BIGSERIAL PRIMARY KEY,
    psi_id TEXT NOT NULL REFERENCES public.psi_investigations(psi_id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    doi TEXT
);

-- 4. Relations between investigations (ground study -> flight data, follow-ons, shared hardware)
CREATE TABLE IF NOT EXISTS public.psi_relations (
    id BIGSERIAL PRIMARY KEY,
    from_psi_id TEXT NOT NULL REFERENCES public.psi_investigations(psi_id) ON DELETE CASCADE,
    to_psi_id TEXT NOT NULL REFERENCES public.psi_investigations(psi_id) ON DELETE CASCADE,
    relation_type TEXT NOT NULL CHECK (relation_type IN ('analyzes_data_of','follow_on_of','reflight_of','risk_mitigation_for','shares_hardware'))
);

-- 5. Habitat atmosphere presets (for the Habitat Risk Panel)
CREATE TABLE IF NOT EXISTS public.habitat_profiles (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    o2_percent NUMERIC(5,2) NOT NULL,
    pressure_psi NUMERIC(6,2) NOT NULL,
    pressure_atm NUMERIC(6,3) NOT NULL,
    gravity_g NUMERIC(5,3),
    note TEXT
);

-- 6. Historical spaceflight fire incidents (timeline)
CREATE TABLE IF NOT EXISTS public.fire_incidents (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    year INTEGER NOT NULL,
    mission TEXT,
    cause TEXT,
    outcome TEXT,
    lessons TEXT,
    source_url TEXT NOT NULL
);

-- 7. Earth applications (spinoffs)
CREATE TABLE IF NOT EXISTS public.earth_applications (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    related_psi_ids TEXT[] NOT NULL DEFAULT '{}',
    source_url TEXT NOT NULL
);

-- 8. Link existing experiments table to PSI
ALTER TABLE public.experiments ADD COLUMN IF NOT EXISTS psi_id TEXT REFERENCES public.psi_investigations(psi_id) ON DELETE SET NULL;

-- 9. Indexes
CREATE INDEX IF NOT EXISTS idx_psi_inv_category ON public.psi_investigations(safety_category);
CREATE INDEX IF NOT EXISTS idx_psi_inv_fuel_state ON public.psi_investigations(fuel_state);
CREATE INDEX IF NOT EXISTS idx_psi_inv_hw_group ON public.psi_investigations(hardware_group);
CREATE INDEX IF NOT EXISTS idx_psi_inv_o2 ON public.psi_investigations(o2_min, o2_max);
CREATE INDEX IF NOT EXISTS idx_psi_tc_psi ON public.psi_test_conditions(psi_id);
CREATE INDEX IF NOT EXISTS idx_psi_pub_psi ON public.psi_publications(psi_id);
CREATE INDEX IF NOT EXISTS idx_experiments_psi_id ON public.experiments(psi_id);

-- 10. RLS: public read-only
DO $$
DECLARE t TEXT;
BEGIN
  FOREACH t IN ARRAY ARRAY['psi_investigations','psi_test_conditions','psi_publications','psi_relations','habitat_profiles','fire_incidents','earth_applications'] LOOP
    EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', t);
    EXECUTE format('DROP POLICY IF EXISTS "public read %s" ON public.%I', t, t);
    EXECUTE format('CREATE POLICY "public read %s" ON public.%I FOR SELECT TO anon, authenticated USING (true)', t, t);
  END LOOP;
END $$;

-- 11. Overview view (catalog + comparison screens)
CREATE OR REPLACE VIEW public.psi_investigation_overview
WITH (security_invoker = true) AS
SELECT
    i.*,
    EXTRACT(YEAR FROM i.start_date)::int AS start_year,
    (SELECT COALESCE(json_agg(json_build_object('psi_id', r.to_psi_id, 'type', r.relation_type)), '[]'::json)
       FROM public.psi_relations r WHERE r.from_psi_id = i.psi_id) AS related_to,
    (SELECT COALESCE(json_agg(json_build_object('psi_id', r.from_psi_id, 'type', r.relation_type)), '[]'::json)
       FROM public.psi_relations r WHERE r.to_psi_id = i.psi_id) AS related_from
FROM public.psi_investigations i;

GRANT SELECT ON public.psi_investigation_overview TO anon, authenticated, service_role;

-- 12. Habitat ranking RPC
--    Ranks investigations for a habitat preset (or custom O2 % / pressure).
--    score = editorial relevance (0-10) * 10, + up to 20 bonus when the tested O2/pressure
--    envelope covers the habitat atmosphere, + 5 for safety categories (prevention/detection/suppression).
CREATE OR REPLACE FUNCTION public.rank_investigations_for_habitat(
    p_habitat_id TEXT DEFAULT 'lunar_base',
    p_o2_percent NUMERIC DEFAULT NULL,
    p_pressure_atm NUMERIC DEFAULT NULL
)
RETURNS TABLE (
    psi_id TEXT, acronym TEXT, title TEXT, safety_category TEXT, fuel_state TEXT,
    o2_min NUMERIC, o2_max NUMERIC, pressure_min_atm NUMERIC, pressure_max_atm NUMERIC,
    relevance SMALLINT, covers_o2 BOOLEAN, covers_pressure BOOLEAN, score NUMERIC, relevance_note TEXT, source_url TEXT
)
LANGUAGE sql STABLE AS $$
WITH h AS (
    SELECT COALESCE(p_o2_percent, hp.o2_percent) AS o2,
           COALESCE(p_pressure_atm, hp.pressure_atm) AS p,
           p_habitat_id AS hid
    FROM (SELECT 1) x LEFT JOIN public.habitat_profiles hp ON hp.id = p_habitat_id
)
SELECT i.psi_id, i.acronym, i.title, i.safety_category, i.fuel_state,
       i.o2_min, i.o2_max, i.pressure_min_atm, i.pressure_max_atm,
       rel.v AS relevance,
       (h.o2 IS NOT NULL AND i.o2_max IS NOT NULL AND h.o2 BETWEEN i.o2_min AND i.o2_max) AS covers_o2,
       (h.p IS NOT NULL AND i.pressure_max_atm IS NOT NULL AND h.p BETWEEN i.pressure_min_atm AND i.pressure_max_atm) AS covers_pressure,
       (COALESCE(rel.v,0) * 10
         + CASE WHEN h.o2 IS NOT NULL AND i.o2_max IS NOT NULL AND h.o2 BETWEEN i.o2_min AND i.o2_max THEN 10 ELSE 0 END
         + CASE WHEN h.p IS NOT NULL AND i.pressure_max_atm IS NOT NULL AND h.p BETWEEN i.pressure_min_atm AND i.pressure_max_atm THEN 10 ELSE 0 END
         + CASE WHEN i.safety_category IN ('prevention','detection','suppression') THEN 5 ELSE 0 END)::NUMERIC AS score,
       i.relevance_note, i.source_url
FROM public.psi_investigations i
CROSS JOIN h
CROSS JOIN LATERAL (SELECT CASE h.hid
        WHEN 'iss' THEN i.relevance_station
        WHEN 'earth' THEN i.relevance_station
        WHEN 'lunar_base' THEN i.relevance_lunar_base
        WHEN 'exploration_26' THEN i.relevance_lunar_base
        WHEN 'exploration_34' THEN i.relevance_lunar_base
        WHEN 'suit' THEN i.relevance_rover
        WHEN 'mars_base' THEN i.relevance_mars_base
        WHEN 'rover' THEN i.relevance_rover
        ELSE i.relevance_spacecraft END AS v) rel
ORDER BY score DESC, i.psi_id;
$$;

GRANT EXECUTE ON FUNCTION public.rank_investigations_for_habitat TO anon, authenticated, service_role;

-- 13. Allow 'psi' rows in experiments to carry the ground/microgravity label (already free text) - no change needed.
