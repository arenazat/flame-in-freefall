// ==============================================================================
// Flame in Freefall — Supabase Edge Function: ask
// Faz 2.2: RAG Soru-Cevap Servisi (Deno / TypeScript)
// ==============================================================================

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { aiProvider, chat, embedQuery } from "../_shared/ai.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

interface AskRequest {
  question?: string;
  query?: string;
  match_threshold?: number;
  match_count?: number;
}

interface SourceReference {
  experimentId: string;
  title: string;
  series: string;
  sourceUrl: string;
  similarity: number;
  excerpt: string;
}

serve(async (req: Request) => {
  // CORS Preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    let body: AskRequest;
    try {
      body = await req.json();
    } catch {
      return new Response(
        JSON.stringify({ error: "Invalid JSON body." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const question = (body.question || body.query || "").trim();
    const matchThreshold = body.match_threshold ?? 0.25;
    const matchCount = body.match_count ?? 8;

    if (!question) {
      return new Response(
        JSON.stringify({ error: "The 'question' parameter is required and cannot be empty." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const provider = aiProvider();
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || Deno.env.get("SUPABASE_ANON_KEY");

    if (!provider) {
      return new Response(
        JSON.stringify({
          answer: "The question could not be answered because the AI key is not configured.",
          sources: [],
          status: "config_error",
          error: "GEMINI_API_KEY (veya OPENAI_API_KEY) secret'ı tanımlı değil.",
        }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (!supabaseUrl || !supabaseServiceKey) {
      return new Response(
        JSON.stringify({
          answer: "Supabase connection parameters are missing.",
          sources: [],
          status: "config_error",
          error: "SUPABASE_URL veya anahtar tanımlı değil.",
        }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    // 1. Soru metninin 1536-boyutlu embedding vektörü (Gemini veya OpenAI)
    let queryEmbedding: number[] | undefined;
    try {
      queryEmbedding = await embedQuery(question);
    } catch (e: any) {
      return new Response(
        JSON.stringify({
          answer: "Could not create the question embedding.",
          sources: [],
          status: "embedding_error",
          error: e.message,
        }),
        { status: 502, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (!queryEmbedding) {
      return new Response(
        JSON.stringify({
          answer: "No valid embedding vector was returned.",
          sources: [],
          status: "embedding_error",
        }),
        { status: 502, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 2. Postgres RPC ile pgvector kosinüs benzerliği araması yap
    let matchedSources: SourceReference[] = [];
    let contextChunks: { text: string; refIndex: number }[] = [];

    // Önce detaylı RPC'yi (metadata içeren) dene
    const { data: rpcData, error: rpcError } = await supabase.rpc(
      "match_experiment_chunks_with_metadata",
      {
        query_embedding: queryEmbedding,
        match_threshold: matchThreshold,
        match_count: matchCount,
      }
    );

    if (!rpcError && Array.isArray(rpcData) && rpcData.length > 0) {
      matchedSources = rpcData.map((row: any) => ({
        experimentId: row.experiment_id,
        title: row.experiment_title,
        series: row.experiment_series,
        sourceUrl: row.experiment_source_url,
        similarity: Number(row.similarity.toFixed(4)),
        excerpt: row.chunk_text.slice(0, 300).trim(),
      }));

      contextChunks = rpcData.map((row: any, i: number) => ({
        text: `[SOURCE ${i + 1}] ID: ${row.experiment_id} | Title: ${row.experiment_title} | Series: ${row.experiment_series} | URL: ${row.experiment_source_url}\nText: ${row.chunk_text}`,
        refIndex: i + 1,
      }));
    } else {
      // Yedek olarak temel match_experiment_chunks RPC'sini ve experiments tablosu join'ini çalıştır
      const { data: basicChunks, error: basicError } = await supabase.rpc(
        "match_experiment_chunks",
        {
          query_embedding: queryEmbedding,
          match_threshold: matchThreshold,
          match_count: matchCount,
        }
      );

      if (basicError || !basicChunks || basicChunks.length === 0) {
        // Kural: Hiçbir zaman kaynak gösterilemeyen bir cevap üretilmesin
        return new Response(
          JSON.stringify({
            answer: "Insufficient data to answer this question.",
            sources: [],
            status: "insufficient_data",
            matchCount: 0,
          }),
          { headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      const experimentIds = [...new Set(basicChunks.map((c: any) => c.experiment_id))];
      const { data: expList } = await supabase
        .from("experiments")
        .select("id, title, series, source_url")
        .in("id", experimentIds);

      const expMap = new Map((expList || []).map((e: any) => [e.id, e]));

      matchedSources = basicChunks.map((chunk: any) => {
        const exp = expMap.get(chunk.experiment_id);
        return {
          experimentId: chunk.experiment_id,
          title: exp?.title || "Bilinmeyen Deney",
          series: exp?.series || "Bilinmeyen Seri",
          sourceUrl: exp?.source_url || "",
          similarity: Number(chunk.similarity.toFixed(4)),
          excerpt: chunk.chunk_text.slice(0, 300).trim(),
        };
      });

      contextChunks = basicChunks.map((chunk: any, i: number) => {
        const exp = expMap.get(chunk.experiment_id);
        const title = exp?.title || "Unknown record";
        const series = exp?.series || "";
        const sourceUrl = exp?.source_url || "";
        return {
          text: `[SOURCE ${i + 1}] ID: ${chunk.experiment_id} | Record: ${title} (${series}) | URL: ${sourceUrl}\nText: ${chunk.chunk_text}`,
          refIndex: i + 1,
        };
      });
    }

    // Kaynak bulunamadıysa kural gereği kesinlikle uydurma cevap verilmez
    if (matchedSources.length === 0) {
      return new Response(
        JSON.stringify({
          answer: "Insufficient data to answer this question.",
          sources: [],
          status: "insufficient_data",
          matchCount: 0,
        }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 3. LLM Çağrısı (gpt-4o-mini)
    const contextText = contextChunks.map((c) => c.text).join("\n\n---\n\n");
    const NO_DATA = "Insufficient data to answer this question.";

    const systemPrompt = `You are a science assistant for NASA microgravity combustion experiments (FLEX, SAFFIRE, BASS, ACME, CIR).

STRICT RULES:
1. Use ONLY the real text provided in the CONTEXT.
2. Never invent, guess or fill in information from general knowledge that is not in the context.
3. If the context is PARTLY relevant (e.g. the experiment's name, objective, observations or conditions), answer with that information and cite it; state clearly which parts "are not covered by the sources".
   Only if the context is COMPLETELY unrelated, reply with one sentence: "Insufficient data to answer this question."
4. Support every claim with the experiment ID, title and source URL from the context.
5. Never produce an answer that cannot be cited.
6. Write the answer in the SAME LANGUAGE as the user's question (English by default), in a clear, scientific style.`;

    let answer: string;
    try {
      answer = (await chat(
        systemPrompt,
        `CONTEXT (NASA MICROGRAVITY EXPERIMENT TEXTS):\n${contextText}\n\nUSER QUESTION:\n${question}`,
        0.1
      )) || NO_DATA;
    } catch (e: any) {
      return new Response(
        JSON.stringify({
          answer: "An error occurred while generating the answer.",
          sources: matchedSources,
          status: "llm_error",
          error: e.message,
        }),
        { status: 502, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Eğer model veri yetersiz dedi veya kaynak referansı taşımıyorsa kuralı uygula
    // Sadece model açıkça ve kısa biçimde reddettiyse 'yetersiz veri' say
    const lower = answer.toLowerCase();
    const isInsufficient =
      answer.length < 200 &&
      (lower.includes("insufficient data") ||
        lower.includes("yeterli veri bulunamadı") ||
        lower.includes("bilgi bulunmamaktadır"));

    return new Response(
      JSON.stringify({
        answer: answer,
        sources: matchedSources,
        status: isInsufficient ? "insufficient_data" : "ok",
        matchCount: matchedSources.length,
      }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err: any) {
    return new Response(
      JSON.stringify({
        answer: "A server error occurred while processing the question.",
        sources: [],
        status: "error",
        error: err.message || "Bilinmeyen bir hata oluştu.",
      }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
