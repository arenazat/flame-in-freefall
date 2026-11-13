// ==============================================================================
// Flame in Freefall — Ortak AI yardımcıları (Edge Functions)
// Embedding: Supabase yerleşik gte-small (384 boyut, anahtarsız, ücretsiz) — veritabanı vector(384).
// Metin üretimi: GEMINI_API_KEY tanımlıysa Google Gemini, değilse OPENAI_API_KEY ile OpenAI.
// ==============================================================================

const GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/models";

export function aiProvider(): "gemini" | "openai" | null {
  if (Deno.env.get("GEMINI_API_KEY")) return "gemini";
  if (Deno.env.get("OPENAI_API_KEY")) return "openai";
  return null;
}

function normalize(v: number[]): number[] {
  const n = Math.sqrt(v.reduce((s, x) => s + x * x, 0));
  return n > 0 ? v.map((x) => x / n) : v;
}

// deno-lint-ignore no-explicit-any
let gteSession: any = null;

/**
 * gte-small yalnızca İngilizce anlar; NASA metinleri de İngilizce.
 * Türkçe (veya başka dilde) soruyu aramadan önce İngilizce arama ifadesine çevirir.
 * Çeviri başarısız olursa orijinal metin kullanılır.
 */
export async function toEnglishSearchQuery(text: string): Promise<string> {
  if (!aiProvider()) return text;
  // Yalnızca ASCII karakter içeren soru büyük olasılıkla zaten İngilizcedir: ekstra Gemini çağrısı yapma
  if (/^[\x00-\x7F]*$/.test(text)) return text;
  try {
    const out = await chat(
      "Translate the user's question into a concise English search query for NASA microgravity combustion papers. Keep technical terms and acronyms. Output ONLY the English query, nothing else.",
      text,
      0,
    );
    return out && out.length > 2 ? out : text;
  } catch {
    return text;
  }
}

/** Soru metni için 384 boyutlu gte-small embedding (ingestion `embed` ile aynı model). */
export async function embedQuery(text: string): Promise<number[]> {
  // deno-lint-ignore no-explicit-any
  if (!gteSession) gteSession = new (globalThis as any).Supabase.ai.Session("gte-small");
  const english = await toEnglishSearchQuery(text);
  const out = await gteSession.run(english.slice(0, 4000), { mean_pool: true, normalize: true });
  return Array.from(out as ArrayLike<number>);
}

/** (Kullanılmıyor) Harici sağlayıcıyla 1536 boyutlu embedding — vector(1536) şemasına dönülürse. */
export async function embedQueryExternal(text: string): Promise<number[]> {
  const provider = aiProvider();
  if (provider === "gemini") {
    const res = await fetch(`${GEMINI_BASE}/gemini-embedding-001:embedContent`, {
      method: "POST",
      headers: { "x-goog-api-key": Deno.env.get("GEMINI_API_KEY")!, "Content-Type": "application/json" },
      body: JSON.stringify({
        content: { parts: [{ text }] },
        taskType: "RETRIEVAL_QUERY",
        outputDimensionality: 1536,
      }),
    });
    if (!res.ok) throw new Error(`Gemini embedding hatası (${res.status}): ${await res.text()}`);
    const json = await res.json();
    const values: number[] | undefined = json.embedding?.values;
    if (!values) throw new Error("Gemini embedding yanıtı boş.");
    return normalize(values);
  }
  if (provider === "openai") {
    const res = await fetch("https://api.openai.com/v1/embeddings", {
      method: "POST",
      headers: { Authorization: `Bearer ${Deno.env.get("OPENAI_API_KEY")}`, "Content-Type": "application/json" },
      body: JSON.stringify({ model: "text-embedding-3-small", input: text }),
    });
    if (!res.ok) throw new Error(`OpenAI embedding hatası (${res.status}): ${await res.text()}`);
    const json = await res.json();
    return json.data[0].embedding;
  }
  throw new Error("AI anahtarı tanımlı değil (GEMINI_API_KEY veya OPENAI_API_KEY).");
}

/** Sistem talimatı + kullanıcı mesajıyla metin cevap üretir. */
export async function chat(systemPrompt: string, userText: string, temperature = 0.1): Promise<string> {
  const provider = aiProvider();
  if (provider === "gemini") {
    // Yoğunluk (503/429) olursa sıradaki modele geç
    const primary = Deno.env.get("GEMINI_CHAT_MODEL") || "gemini-flash-latest";
    const models = [...new Set([primary, "gemini-flash-lite-latest", "gemini-flash-latest"])];
    let lastErr = "";
    for (const model of models) {
      const res = await fetch(`${GEMINI_BASE}/${model}:generateContent`, {
        method: "POST",
        headers: { "x-goog-api-key": Deno.env.get("GEMINI_API_KEY")!, "Content-Type": "application/json" },
        body: JSON.stringify({
          systemInstruction: { parts: [{ text: systemPrompt }] },
          contents: [{ role: "user", parts: [{ text: userText }] }],
          generationConfig: { temperature },
        }),
      });
      if (res.ok) {
        const json = await res.json();
        const parts = json.candidates?.[0]?.content?.parts ?? [];
        return parts.map((p: any) => p.text ?? "").join("").trim();
      }
      lastErr = `Gemini metin hatası (${res.status}, ${model}): ${await res.text()}`;
      if (![429, 500, 503].includes(res.status)) break;
    }
    throw new Error(lastErr);
  }
  if (provider === "openai") {
    const res = await fetch("https://api.openai.com/v1/chat/completions", {
      method: "POST",
      headers: { Authorization: `Bearer ${Deno.env.get("OPENAI_API_KEY")}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        model: "gpt-4o-mini",
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: userText },
        ],
        temperature,
      }),
    });
    if (!res.ok) throw new Error(`OpenAI metin hatası (${res.status}): ${await res.text()}`);
    const json = await res.json();
    return (json.choices?.[0]?.message?.content ?? "").trim();
  }
  throw new Error("AI anahtarı tanımlı değil (GEMINI_API_KEY veya OPENAI_API_KEY).");
}
