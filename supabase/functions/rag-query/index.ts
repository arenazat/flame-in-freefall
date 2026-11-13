// ==============================================================================
// Flame in Freefall — Supabase Edge Function: rag-query
// RAG (Retrieval-Augmented Generation) Soru-Cevap Servisi
// Deno / TypeScript
// ==============================================================================

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { aiProvider, chat, embedQuery } from "../_shared/ai.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

interface RagRequest {
  query: string;
  top_k?: number;
}

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { query, top_k = 5 }: RagRequest = await req.json();

    if (!query || query.trim() === "") {
      return new Response(
        JSON.stringify({ error: "The 'query' parameter is required." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const provider = aiProvider();
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

    if (!provider) {
      return new Response(
        JSON.stringify({ error: "GEMINI_API_KEY (veya OPENAI_API_KEY) secret'ı tanımlı değil." }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (!supabaseUrl || !supabaseServiceKey) {
      return new Response(
        JSON.stringify({ error: "Supabase connection parameters are missing." }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    // 1. Sorgunun 1536 boyutlu embedding vektörü (Gemini veya OpenAI)
    let queryEmbedding: number[];
    try {
      queryEmbedding = await embedQuery(query);
    } catch (e: any) {
      return new Response(
        JSON.stringify({ error: e.message }),
        { status: 502, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 2. Supabase RPC 'match_experiment_chunks' ile pgvector kosinüs benzerliği araması yap
    const { data: matchedChunks, error: rpcError } = await supabase.rpc(
      "match_experiment_chunks",
      {
        query_embedding: queryEmbedding,
        match_threshold: 0.25,
        match_count: top_k,
      }
    );

    if (rpcError) {
      return new Response(
        JSON.stringify({ error: `Vector search error: ${rpcError.message}` }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (!matchedChunks || matchedChunks.length === 0) {
      return new Response(
        JSON.stringify({
          answer: "Insufficient data to answer this question.",
          sources: [],
          matchCount: 0,
        }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 3. Eşleşen deneylerin üst veri detaylarını çek
    const experimentIds = [...new Set(matchedChunks.map((c: any) => c.experiment_id))];
    const { data: experimentDetails } = await supabase
      .from("experiments")
      .select("id, title, series, material, gravity_level, year, source_url")
      .in("id", experimentIds);

    const experimentMap = new Map((experimentDetails || []).map((e: any) => [e.id, e]));

    // 4. OpenAI Chat ile sadece gerçek metin bağlamına dayalı yanıt sentezle
    const contextText = matchedChunks
      .map((chunk: any, i: number) => {
        const exp = experimentMap.get(chunk.experiment_id);
        const expTitle = exp ? exp.title : "Unknown record";
        const expSeries = exp ? exp.series : "Unknown series";
        return `[SOURCE ${i + 1}] Record: ${expTitle} (${expSeries})\nContent: ${chunk.chunk_text}`;
      })
      .join("\n\n---\n\n");

    const systemPrompt = `You are a science assistant specialized in NASA microgravity combustion experiments (FLEX, SAFFIRE, BASS and others).
STRICT RULES:
1. Answer ONLY with information from the provided context texts.
2. Never invent or guess anything that is not in the context.
3. If the information is insufficient, say clearly: "Insufficient data to answer this question."
4. Name the record titles and series you cite.
5. Answer in the same language as the user's question (English by default), clearly and scientifically.`;

    let answer: string;
    try {
      answer = (await chat(systemPrompt, `CONTEXT:\n${contextText}\n\nUSER QUESTION: ${query}`, 0.1)) || "No answer could be generated.";
    } catch (e: any) {
      return new Response(
        JSON.stringify({ error: e.message }),
        { status: 502, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const sources = matchedChunks.map((chunk: any) => {
      const exp = experimentMap.get(chunk.experiment_id);
      return {
        chunkId: chunk.id,
        experimentId: chunk.experiment_id,
        experimentTitle: exp ? exp.title : "",
        series: exp ? exp.series : "",
        sourceUrl: exp ? exp.source_url : "",
        similarity: chunk.similarity,
        previewText: chunk.chunk_text.slice(0, 200) + "...",
      };
    });

    return new Response(
      JSON.stringify({
        answer,
        sources,
        matchCount: matchedChunks.length,
      }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error: any) {
    return new Response(
      JSON.stringify({ error: error.message || "An unknown error occurred." }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
