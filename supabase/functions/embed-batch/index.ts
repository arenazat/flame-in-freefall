// ==============================================================================
// Flame in Freefall — Edge Function: embed-batch
// Supabase'in yerleşik gte-small modeliyle (384 boyut) metin embedding'i üretir.
// Harici API, anahtar veya kota gerektirmez. Ingestion CLI `embed` komutu bunu çağırır.
// İstek:  { "texts": ["...", "..."] }   (en fazla 16 metin, her biri en fazla 4000 karakter)
// Yanıt:  { "embeddings": [[...384 sayı...], ...] }
// ==============================================================================

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

// deno-lint-ignore no-explicit-any
const model = new (globalThis as any).Supabase.ai.Session("gte-small");

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  try {
    const body = await req.json();
    const texts: unknown = body?.texts;
    if (!Array.isArray(texts) || texts.length === 0 || texts.length > 16) {
      return new Response(JSON.stringify({ error: "'texts' 1-16 elemanlı bir dizi olmalı." }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }
    const embeddings: number[][] = [];
    for (const t of texts) {
      const text = String(t ?? "").slice(0, 4000);
      const out = await model.run(text, { mean_pool: true, normalize: true });
      embeddings.push(Array.from(out as ArrayLike<number>));
    }
    return new Response(JSON.stringify({ embeddings }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (e) {
    return new Response(JSON.stringify({ error: (e as Error).message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
