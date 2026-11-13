-- ==============================================================================
-- Faz 4.3 — Rapor Oluşturucu için Supabase Storage ve İzin Yapılandırması
-- Migration: 20260929000004_create_reports_bucket.sql
-- ==============================================================================

-- 1. 'reports' Depolama Kovasını (Storage Bucket) Oluştur
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'reports',
    'reports',
    true,
    10485760, -- 10 MB Maksimum Dosya Boyutu
    ARRAY['text/html', 'application/pdf', 'application/json']
)
ON CONFLICT (id) DO UPDATE SET
    public = true,
    file_size_limit = 10485760;

-- 2. Depolama Güvenlik Politikaları (RLS)
-- Herkes oluşturulan raporları salt-okunur olarak indirebilir / görüntüleyebilir
DROP POLICY IF EXISTS "Allow public read on reports" ON storage.objects;
CREATE POLICY "Allow public read on reports"
    ON storage.objects
    FOR SELECT
    TO anon, authenticated
    USING (bucket_id = 'reports');

-- Raporları Edge Function veya yetkili istemciler yükleyebilir
DROP POLICY IF EXISTS "Allow insert on reports" ON storage.objects;
CREATE POLICY "Allow insert on reports"
    ON storage.objects
    FOR INSERT
    TO anon, authenticated, service_role
    WITH CHECK (bucket_id = 'reports');
