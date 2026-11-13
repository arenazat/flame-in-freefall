// ==============================================================================
// Flame in Freefall — Supabase Edge Function: generate-report
// Faz 4.3: Bilimsel Rapor Oluşturucu (Deno / TypeScript)
// ==============================================================================

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

interface ReportRequest {
  experiment_ids?: string[];
  title?: string;
}

interface ExperimentRow {
  id: string;
  title: string;
  series: string;
  material: string;
  gravity_level: string;
  oxygen_concentration: number | null;
  year: number | null;
  source_url: string;
  source_type: string;
  raw_text: string | null;
  summary_id: string | null;
  ai_summary: string | null;
  risk_score: number | null;
  risk_label: string | null;
  key_finding: string | null;
  summary_status: string | null;
  one_g_value: number | null;
  micro_g_value: number | null;
  comparison_unit: string | null;
  comparison_metric_name: string | null;
  comparison_notes: string | null;
}

serve(async (req: Request) => {
  // CORS Preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    let body: ReportRequest;
    try {
      body = await req.json();
    } catch {
      return new Response(
        JSON.stringify({ error: "Invalid JSON request body." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const experimentIds = body.experiment_ids || [];
    if (!Array.isArray(experimentIds) || experimentIds.length === 0) {
      return new Response(
        JSON.stringify({ error: "At least one valid record ID ('experiment_ids') must be provided." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || Deno.env.get("SUPABASE_ANON_KEY");

    if (!supabaseUrl || !supabaseServiceKey) {
      return new Response(
        JSON.stringify({ error: "Supabase server configuration is missing." }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    // 1. Seçilen GERÇEK deney kayıtlarını Supabase'den çek
    const { data: experiments, error: queryError } = await supabase
      .from("experiment_details_view")
      .select("*")
      .in("id", experimentIds);

    if (queryError) {
      return new Response(
        JSON.stringify({ error: `Database query error: ${queryError.message}` }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (!experiments || experiments.length === 0) {
      return new Response(
        JSON.stringify({ error: "No records found for the selected IDs." }),
        { status: 404, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const rows: ExperimentRow[] = experiments;

    // 2. İstatistikleri Hesapla
    const reportId = `REP-${Date.now().toString(36).toUpperCase()}-${Math.random().toString(36).substring(2, 6).toUpperCase()}`;
    const generationDate = new Date().toUTCString();
    const reportTitle = body.title || "NASA Microgravity Combustion Experiments Analysis Report";

    const scoredExperiments = rows.filter((r) => typeof r.risk_score === "number" && r.risk_score >= 0);
    const avgRisk = scoredExperiments.length > 0
      ? Math.round(scoredExperiments.reduce((sum, r) => sum + (r.risk_score || 0), 0) / scoredExperiments.length)
      : null;

    const materials = Array.from(new Set(rows.map((r) => r.material).filter(Boolean)));
    const series = Array.from(new Set(rows.map((r) => r.series).filter(Boolean)));

    // 3. Yazdırılabilir & İndirilebilir Profesyonel HTML Şablonunu Oluştur
    const html = generateHtmlReport({
      reportId,
      reportTitle,
      generationDate,
      rows,
      avgRisk,
      materials,
      series,
    });

    const filename = `NASA_FlameFall_${reportId}.html`;

    // 4. HTML Raporunu Supabase Storage 'reports' Kovasına Yükle
    let publicUrl = "";
    try {
      const { error: uploadError } = await supabase.storage
        .from("reports")
        .upload(filename, new Blob([html], { type: "text/html; charset=utf-8" }), {
          contentType: "text/html; charset=utf-8",
          upsert: true,
        });

      if (!uploadError) {
        const { data: urlData } = supabase.storage.from("reports").getPublicUrl(filename);
        publicUrl = urlData.publicUrl;
      } else {
        console.warn("[STORAGE UYARI] Depolama yükleme hatası:", uploadError.message);
      }
    } catch (storageErr) {
      console.warn("[STORAGE HATA]", storageErr);
    }

    // Depolama URL'si oluşturulamadıysa Data URL fallback'i hazırla
    if (!publicUrl) {
      const base64Html = btoa(unescape(encodeURIComponent(html)));
      publicUrl = `data:text/html;charset=utf-8;base64,${base64Html}`;
    }

    return new Response(
      JSON.stringify({
        report_id: reportId,
        report_url: publicUrl,
        filename: filename,
        experiment_count: rows.length,
        created_at: new Date().toISOString(),
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    return new Response(
      JSON.stringify({ error: `Unexpected error while generating the report: ${message}` }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});

interface TemplateData {
  reportId: string;
  reportTitle: string;
  generationDate: string;
  rows: ExperimentRow[];
  avgRisk: number | null;
  materials: string[];
  series: string[];
}

function generateHtmlReport(data: TemplateData): string {
  const { reportId, reportTitle, generationDate, rows, avgRisk, materials, series } = data;
  const scoredExperiments = rows.filter((r) => typeof r.risk_score === "number" && r.risk_score >= 0);

  const experimentCardsHtml = rows
    .map((exp, idx) => {
      const riskClass = exp.risk_score !== null
        ? exp.risk_score < 35
          ? "risk-low"
          : exp.risk_score < 70
          ? "risk-med"
          : "risk-high"
        : "risk-none";

      const riskLabel = exp.risk_label || (exp.risk_score !== null ? `${exp.risk_score}/100` : "Insufficient data");

      const comparativeBlock = (exp.one_g_value && exp.micro_g_value)
        ? `
        <div class="comparative-box">
          <div class="comp-title">1g vs microgravity comparative measurement: <strong>${exp.comparison_metric_name || "Flame spread rate"}</strong></div>
          <div class="comp-grid">
            <div class="comp-item comp-1g">
              <span class="comp-lbl">1g Earth (9.81 m/s²):</span>
              <span class="comp-val">${exp.one_g_value} ${exp.comparison_unit || "mm/s"}</span>
            </div>
            <div class="comp-item comp-ug">
              <span class="comp-lbl">Microgravity (μg):</span>
              <span class="comp-val">${exp.micro_g_value} ${exp.comparison_unit || "mm/s"}</span>
            </div>
          </div>
          ${exp.comparison_notes ? `<div class="comp-notes">${exp.comparison_notes}</div>` : ""}
        </div>
        `
        : "";

      return `
      <div class="experiment-card">
        <div class="exp-header">
          <div class="exp-index">#${idx + 1}</div>
          <div class="exp-title-block">
            <h3 class="exp-title">${escapeHtml(exp.title)}</h3>
            <div class="exp-meta-pills">
              <span class="pill pill-series">${escapeHtml(exp.series)}</span>
              <span class="pill pill-material">${escapeHtml(exp.material)}</span>
              <span class="pill pill-neutral">${exp.year || "Year not specified"}</span>
              <span class="pill pill-neutral">Gravity: ${escapeHtml(exp.gravity_level)}</span>
              <span class="pill pill-neutral">O₂: ${exp.oxygen_concentration ?? 21.0}%</span>
            </div>
          </div>
          <div class="exp-risk-badge ${riskClass}">
            <span class="risk-val">${exp.risk_score !== null ? exp.risk_score : "—"}</span>
            <span class="risk-lbl">${escapeHtml(riskLabel)}</span>
          </div>
        </div>

        <div class="exp-body">
          <div class="section-label">AI SCIENTIFIC SUMMARY</div>
          <p class="summary-text">${escapeHtml(exp.ai_summary || "Full-text analysis for this record is not complete yet.")}</p>

          ${exp.key_finding ? `
            <div class="finding-box">
              <strong>Key finding & safety implication:</strong> ${escapeHtml(exp.key_finding)}
            </div>
          ` : ""}

          ${comparativeBlock}

          <div class="exp-footer">
            <span class="source-tag">Official NASA source (${exp.source_type.toUpperCase()}):</span>
            <a href="${exp.source_url}" target="_blank" class="source-link">${escapeHtml(exp.source_url)}</a>
          </div>
        </div>
      </div>
      `;
    })
    .join("\n");

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>${escapeHtml(reportTitle)} — ${reportId}</title>
  <style>
    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap');

    :root {
      --bg: #F7F8FA;
      --surface: #FFFFFF;
      --border: #E2E5EA;
      --border-subtle: #ECEEF2;
      --accent-orange: #E8572E;
      --accent-teal: #1E88C7;
      --text-main: #1B1F27;
      --text-muted: #6B7280;
      --text-light: #9CA3AF;
      --risk-low: #2E9E6C;
      --risk-low-bg: #EAF6F0;
      --risk-med: #C98A1A;
      --risk-med-bg: #FAF3E6;
      --risk-high: #D64545;
      --risk-high-bg: #FBECEC;
    }

    * { box-sizing: border-box; margin: 0; padding: 0; }

    body {
      font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background-color: var(--bg);
      color: var(--text-main);
      line-height: 1.5;
      padding: 32px 24px;
    }

    .report-container {
      max-width: 960px;
      margin: 0 auto;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 40px;
      box-shadow: 0 4px 16px rgba(0,0,0,0.04);
    }

    .header {
      border-bottom: 2px solid var(--border);
      padding-bottom: 24px;
      margin-bottom: 28px;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }

    .brand-title {
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 1.5px;
      color: var(--accent-orange);
      text-transform: uppercase;
      margin-bottom: 6px;
    }

    .main-title {
      font-size: 24px;
      font-weight: 800;
      color: var(--text-main);
      margin-bottom: 6px;
    }

    .sub-title {
      font-size: 13px;
      color: var(--text-muted);
    }

    .report-meta {
      text-align: right;
      font-size: 12px;
      color: var(--text-muted);
    }

    .meta-badge {
      display: inline-block;
      background: var(--bg);
      border: 1px solid var(--border);
      padding: 6px 12px;
      border-radius: 6px;
      font-weight: 700;
      color: var(--text-main);
      margin-top: 6px;
    }

    .exec-summary {
      background: var(--bg);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 20px;
      margin-bottom: 32px;
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;
    }

    .stat-item {
      display: flex;
      flex-direction: column;
    }

    .stat-label {
      font-size: 11px;
      font-weight: 600;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: 4px;
    }

    .stat-value {
      font-size: 20px;
      font-weight: 800;
      color: var(--text-main);
    }

    .stat-sub {
      font-size: 11px;
      color: var(--text-light);
    }

    .section-header {
      font-size: 14px;
      font-weight: 800;
      letter-spacing: 0.8px;
      color: var(--text-muted);
      text-transform: uppercase;
      margin-bottom: 16px;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .section-header::after {
      content: '';
      flex: 1;
      height: 1px;
      background: var(--border);
    }

    .experiment-card {
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 20px;
      margin-bottom: 20px;
      background: var(--surface);
      page-break-inside: avoid;
    }

    .exp-header {
      display: flex;
      gap: 14px;
      align-items: flex-start;
      margin-bottom: 14px;
    }

    .exp-index {
      font-size: 14px;
      font-weight: 800;
      color: var(--accent-orange);
      background: rgba(232, 87, 46, 0.08);
      padding: 4px 8px;
      border-radius: 4px;
    }

    .exp-title-block {
      flex: 1;
    }

    .exp-title {
      font-size: 15px;
      font-weight: 700;
      color: var(--text-main);
      margin-bottom: 8px;
      line-height: 1.4;
    }

    .exp-meta-pills {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }

    .pill {
      font-size: 10px;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 4px;
      text-transform: uppercase;
    }

    .pill-series { background: rgba(30, 136, 199, 0.12); color: var(--accent-teal); }
    .pill-material { background: rgba(232, 87, 46, 0.1); color: var(--accent-orange); }
    .pill-neutral { background: var(--bg); border: 1px solid var(--border); color: var(--text-muted); }

    .exp-risk-badge {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 8px 12px;
      border-radius: 6px;
      min-width: 80px;
      text-align: center;
    }

    .risk-val { font-size: 16px; font-weight: 800; }
    .risk-lbl { font-size: 9px; font-weight: 700; text-transform: uppercase; }

    .risk-low { background: var(--risk-low-bg); color: var(--risk-low); border: 1px solid rgba(46, 158, 108, 0.3); }
    .risk-med { background: var(--risk-med-bg); color: var(--risk-med); border: 1px solid rgba(201, 138, 26, 0.3); }
    .risk-high { background: var(--risk-high-bg); color: var(--risk-high); border: 1px solid rgba(214, 69, 69, 0.3); }
    .risk-none { background: var(--bg); color: var(--text-muted); border: 1px solid var(--border); }

    .section-label {
      font-size: 10px;
      font-weight: 700;
      color: var(--text-muted);
      letter-spacing: 0.5px;
      margin-bottom: 4px;
    }

    .summary-text {
      font-size: 13px;
      color: var(--text-main);
      line-height: 1.6;
      margin-bottom: 12px;
    }

    .finding-box {
      background: var(--bg);
      border-left: 3px solid var(--accent-teal);
      padding: 10px 14px;
      font-size: 12px;
      color: var(--text-main);
      border-radius: 0 6px 6px 0;
      margin-bottom: 12px;
    }

    .comparative-box {
      background: #F8FAFC;
      border: 1px solid var(--border);
      border-radius: 6px;
      padding: 12px 14px;
      margin-bottom: 12px;
    }

    .comp-title { font-size: 11px; font-weight: 600; color: var(--text-muted); margin-bottom: 6px; }
    .comp-grid { display: flex; gap: 20px; }
    .comp-item { display: flex; gap: 6px; align-items: baseline; font-size: 12px; }
    .comp-lbl { color: var(--text-muted); }
    .comp-val { font-weight: 800; }
    .comp-1g .comp-val { color: var(--accent-teal); }
    .comp-ug .comp-val { color: var(--accent-orange); }
    .comp-notes { font-size: 11px; color: var(--text-muted); margin-top: 6px; font-style: italic; }

    .exp-footer {
      border-top: 1px solid var(--border-subtle);
      padding-top: 10px;
      font-size: 11px;
      display: flex;
      gap: 6px;
      align-items: center;
    }

    .source-tag { color: var(--text-muted); font-weight: 600; }
    .source-link { color: var(--accent-teal); text-decoration: none; word-break: break-all; }
    .source-link:hover { text-decoration: underline; }

    .footer {
      border-top: 2px solid var(--border);
      padding-top: 20px;
      margin-top: 40px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 11px;
      color: var(--text-muted);
    }

    .action-btn-print {
      display: inline-block;
      background: var(--accent-orange);
      color: #fff;
      padding: 8px 16px;
      border-radius: 6px;
      font-size: 12px;
      font-weight: 700;
      text-decoration: none;
      cursor: pointer;
      border: none;
    }

    @media print {
      body { background: #fff; padding: 0; }
      .report-container { border: none; box-shadow: none; padding: 0; max-width: 100%; }
      .no-print { display: none !important; }
      @page { margin: 1.5cm; }
    }
  </style>
</head>
<body>
  <div class="report-container">
    <div class="header">
      <div>
        <div class="brand-title">FLAME IN FREEFALL • NASA MICROGRAVITY ANALYTICS PLATFORM</div>
        <h1 class="main-title">${escapeHtml(reportTitle)}</h1>
        <p class="sub-title">Collection of verified NASA NTRS & PSI technical reports</p>
      </div>
      <div class="report-meta">
        <div>Date: <strong>${generationDate}</strong></div>
        <div class="meta-badge">${reportId}</div>
      </div>
    </div>

    <div class="no-print" style="margin-bottom: 24px; display: flex; justify-content: flex-end;">
      <button onclick="window.print()" class="action-btn-print">🖨️ Print / Save as PDF</button>
    </div>

    <div class="exec-summary">
      <div class="stat-item">
        <span class="stat-label">Total records</span>
        <span class="stat-value">${rows.length}</span>
        <span class="stat-sub">Verified records</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">Average risk score</span>
        <span class="stat-value" style="color: ${avgRisk && avgRisk >= 50 ? 'var(--risk-high)' : 'var(--risk-low)'};">
          ${avgRisk !== null ? `${avgRisk}/100` : '—'}
        </span>
        <span class="stat-sub">${scoredExperiments.length} records assessed</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">Programs covered</span>
        <span class="stat-value">${series.length}</span>
        <span class="stat-sub">${series.join(', ')}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">Materials tested</span>
        <span class="stat-value">${materials.length}</span>
        <span class="stat-sub">${materials.slice(0, 3).join(', ')}${materials.length > 3 ? '...' : ''}</span>
      </div>
    </div>

    <div class="section-header">RECORD DETAILS & SAFETY ANALYSIS (${rows.length})</div>

    <div class="experiments-list">
      ${experimentCardsHtml}
    </div>

    <div class="footer">
      <div>Flame in Freefall • Verified NASA PSI & NTRS data</div>
      <div>This report was compiled automatically from real technical texts. No mock or estimated data is used.</div>
    </div>
  </div>
</body>
</html>`;
}

function escapeHtml(text: string): string {
  if (!text) return "";
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
