"use strict";

const ICONS = {
  noty: '<svg viewBox="232 232 560 560" class="icon-fill" aria-hidden="true"><path d="M304.11 744 L304.11 289.98 L420.53 289.98 L420.53 345.69 Q437.99 312.43 471.25 296.22 Q504.52 280 547.76 280 Q599.31 280 637.98 301.62 Q676.65 323.24 698.27 361.91 Q719.89 400.57 719.89 452.13 L719.89 744 L568.54 744 L548.59 571.04 A93.13 93.13 0 1 0 475.41 571.04 L455.46 744 Z"/></svg>',
  sticker: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 12V7a3 3 0 0 0-3-3H7a3 3 0 0 0-3 3v10a3 3 0 0 0 3 3h5"/><path d="M20 12a8 8 0 0 1-8 8v-3a5 5 0 0 1 5-5z"/><path d="M8.5 13.5c.9.9 2 1.4 3.2 1.5"/><path d="M9 9.2v.6M15 9.2v.6"/></svg>',
  cube: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2.8 20 7.4v9.2l-8 4.6-8-4.6V7.4z"/><path d="m4 7.4 8 4.6 8-4.6M12 12v9.2"/><path d="m8 5.1 8 4.6M16 5.1 8 9.7"/></svg>',
  letters: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3.5" y="3.5" width="17" height="17" rx="3.5"/><path d="m8.4 16.2 3.6-8.4 3.6 8.4M9.7 13.3h4.6"/></svg>',
  compass: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="m15.6 8.4-2.1 5.1-5.1 2.1 2.1-5.1z"/></svg>',
  globe: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="M3.5 12h17M12 3.5c2.4 2.5 3.5 5.3 3.5 8.5s-1.1 6-3.5 8.5c-2.4-2.5-3.5-5.3-3.5-8.5s1.1-6 3.5-8.5z"/></svg>',
};

const UI_ICONS = {
  chevron: '<svg class="chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg>',
  external: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M14 4h6v6M20 4l-9 9M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5"/></svg>',
  lock: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="10.5" width="14" height="10" rx="2"/><path d="M8 10.5V8a4 4 0 0 1 8 0v2.5"/></svg>',
  link: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1"/><path d="M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"/></svg>',
  close: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 7l10 10M17 7 7 17"/></svg>',
};

const KINDS = { app: "Uygulamalar", tool: "Rehber ve araçlar", web: "Web" };
const KIND_LABEL = { app: "Uygulama", tool: "Rehber / araç", web: "Web" };
const STATUS_LABEL = { live: "Yayında", dev: "Geliştiriliyor", private: "Özel" };
const PRIVATE_LINK_HINT = "claude.ai hesabımda duruyor; paylaşıma açık değilse yalnızca ben açabilirim.";

const projects = window.PROJECTS;
const filter = { kind: "all", tech: null };

const $ = (id) => document.getElementById(id);
const listEl = $("project-list");

function esc(text) {
  return String(text).replace(/[&<>"']/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
}

function linkAttrs(link) {
  const tip = link.private ? ` title="${esc(PRIVATE_LINK_HINT)}"` : "";
  return `href="${esc(link.url)}" target="_blank" rel="noopener"${tip}`;
}

function tileHtml(p) {
  const glyph = ICONS[p.icon] || `<span class="tile-letter">${esc(p.name.charAt(0))}</span>`;
  return `<span class="tile" style="--tile:${esc(p.color || "var(--accent)")};--tile-ink:${esc(p.ink || "#fff")}">${glyph}</span>`;
}

function factsHtml(p) {
  const rows = [
    ["Tür", esc(KIND_LABEL[p.kind])],
    ["Platform", esc(p.platforms.join(", "))],
  ];
  if (p.tech.length) {
    rows.push(["Teknoloji", p.tech.map((t) => `<button type="button" class="tag tag-btn" data-tech="${esc(t)}">${esc(t)}</button>`).join("")]);
  }
  if (p.version) rows.push(["Sürüm", `<span class="mono">${esc(p.version)}</span>`]);
  if (p.started) rows.push(["Başlangıç", esc(p.started)]);
  if (p.security) rows.push(["Güvenlik", esc(p.security), "wide"]);
  return `<dl class="facts">${rows.map(([k, v, cls]) => `<div${cls ? ` class="${cls}"` : ""}><dt>${k}</dt><dd>${v}</dd></div>`).join("")}</dl>`;
}

function linksHtml(p) {
  const buttons = p.links.map((link) => {
    const icon = link.private ? UI_ICONS.lock : UI_ICONS.external;
    const suffix = link.private ? ' <span class="link-note">özel</span>' : "";
    return `<a class="btn btn-sm${link.private ? " btn-private" : ""}" ${linkAttrs(link)}>${icon}${esc(link.label)}${suffix}</a>`;
  });
  buttons.push(`<button type="button" class="btn btn-sm btn-ghost" data-copy="${esc(p.id)}">${UI_ICONS.link}Bağlantıyı kopyala</button>`);
  return `<div class="detail-links">${buttons.join("")}</div>`;
}

function projectHtml(p) {
  const quick = p.links.find((link) => !link.private);
  const quickHtml = quick
    ? `<a class="quick" ${linkAttrs(quick)} aria-label="${esc(p.name)}: ${esc(quick.label)}">${esc(quick.label)}${UI_ICONS.external}</a>`
    : "";
  return `
    <article class="project" id="${esc(p.id)}" data-kind="${esc(p.kind)}">
      <div class="project-head">
        ${tileHtml(p)}
        <div class="project-text">
          <h3 class="project-title">
            <button type="button" class="project-toggle" aria-expanded="false" aria-controls="${esc(p.id)}-detay">${esc(p.name)}</button>
            <span class="status status-${p.status}">${STATUS_LABEL[p.status]}</span>
          </h3>
          <p class="project-tagline">${esc(p.tagline)}</p>
          <p class="project-meta">${esc(p.platforms.join(" · "))}</p>
        </div>
        ${quickHtml}
        ${UI_ICONS.chevron}
      </div>
      <div class="project-detail" id="${esc(p.id)}-detay" hidden="until-found">
        <div class="detail-inner">
          <p class="detail-summary">${esc(p.summary)}</p>
          <h4 class="detail-heading">Öne çıkanlar</h4>
          <ul class="feature-list">${p.features.map((f) => `<li>${esc(f)}</li>`).join("")}</ul>
          ${factsHtml(p)}
          ${linksHtml(p)}
        </div>
      </div>
    </article>`;
}

function setExpanded(article, open) {
  const toggle = article.querySelector(".project-toggle");
  const detail = article.querySelector(".project-detail");
  toggle.setAttribute("aria-expanded", String(open));
  article.classList.toggle("is-open", open);
  if (open) detail.removeAttribute("hidden");
  else detail.setAttribute("hidden", "until-found");
  syncExpandAll();
}

function visibleArticles() {
  return [...listEl.querySelectorAll(".project:not([hidden])")];
}

function syncExpandAll() {
  const visible = visibleArticles();
  const allOpen = visible.length > 0 && visible.every((a) => a.classList.contains("is-open"));
  const btn = $("expand-all");
  btn.textContent = allOpen ? "Tümünü kapat" : "Tümünü aç";
  btn.dataset.mode = allOpen ? "close" : "open";
  btn.hidden = visible.length === 0;
}

function matches(p) {
  return (filter.kind === "all" || p.kind === filter.kind) && (!filter.tech || p.tech.includes(filter.tech));
}

function renderFilters() {
  const counts = { all: projects.length };
  for (const p of projects) counts[p.kind] = (counts[p.kind] || 0) + 1;
  const options = [["all", "Hepsi"], ...Object.entries(KINDS).filter(([key]) => counts[key])];
  $("filters").innerHTML = options
    .map(([key, label]) => `<button type="button" class="chip" data-kind="${key}" aria-pressed="${filter.kind === key}">${label}<span class="chip-count">${counts[key]}</span></button>`)
    .join("");
}

function applyFilter() {
  let shown = 0;
  for (const p of projects) {
    const article = $(p.id);
    const ok = matches(p);
    article.hidden = !ok;
    if (ok) shown++;
  }
  $("empty").hidden = shown > 0;

  const active = $("active-filter");
  active.hidden = !filter.tech;
  active.innerHTML = filter.tech
    ? `<span>Teknoloji:</span><button type="button" class="chip chip-active" data-clear-tech aria-label="${esc(filter.tech)} filtresini kaldır">${esc(filter.tech)}${UI_ICONS.close}</button>`
    : "";

  for (const chip of document.querySelectorAll("#filters .chip")) {
    chip.setAttribute("aria-pressed", String(chip.dataset.kind === filter.kind));
  }
  for (const chip of document.querySelectorAll("#tech-list .chip")) {
    chip.setAttribute("aria-pressed", String(chip.dataset.tech === filter.tech));
  }
  syncExpandAll();
}

function renderTech() {
  const counts = new Map();
  for (const p of projects) for (const t of p.tech) counts.set(t, (counts.get(t) || 0) + 1);
  const sorted = [...counts].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0], "tr"));
  $("tech-list").innerHTML = sorted
    .map(([t, n]) => `<button type="button" class="chip" data-tech="${esc(t)}" aria-pressed="false">${esc(t)}<span class="chip-count">${n}</span></button>`)
    .join("");
}

function renderStats() {
  const live = projects.filter((p) => p.status === "live").length;
  const dev = projects.filter((p) => p.status === "dev").length;
  const parts = [`${projects.length} proje`, `${live} yayında`];
  if (dev) parts.push(`${dev} geliştiriliyor`);
  $("hero-stats").textContent = parts.join(" · ");
  $("updated").textContent = `Son güncelleme: ${window.SITE.updated}`;
}

function showTechFilter(tech) {
  filter.tech = filter.tech === tech ? null : tech;
  filter.kind = "all";
  applyFilter();
  $("projeler").scrollIntoView({ behavior: prefersReducedMotion() ? "auto" : "smooth", block: "start" });
}

function prefersReducedMotion() {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

let toastTimer;
function toast(message) {
  const el = $("toast");
  el.textContent = message;
  el.classList.add("is-visible");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove("is-visible"), 1800);
}

async function copyLink(id) {
  const url = `${location.origin}${location.pathname}#${id}`;
  try {
    await navigator.clipboard.writeText(url);
    toast("Bağlantı kopyalandı");
  } catch {
    // Compat: pano izni yoksa (eski tarayıcı, http) adresi çubuğa yazıp kullanıcıya bırak.
    history.replaceState(null, "", `#${id}`);
    toast("Adres çubuğundan kopyalayabilirsin");
  }
}

function openFromHash() {
  const id = decodeURIComponent(location.hash.slice(1));
  if (!id) return;
  if (id === "araclar") {
    $("araclar").open = true;
    return;
  }
  const article = document.getElementById(id);
  if (!article || !article.classList.contains("project")) return;
  if (article.hidden) {
    filter.kind = "all";
    filter.tech = null;
    applyFilter();
  }
  setExpanded(article, true);
  article.scrollIntoView({ block: "start" });
}

function setupTheme() {
  const btn = $("theme-toggle");
  const systemDark = window.matchMedia("(prefers-color-scheme: dark)");
  const current = () => document.documentElement.dataset.theme || (systemDark.matches ? "dark" : "light");
  const sync = () => btn.setAttribute("aria-label", current() === "dark" ? "Açık temaya geç" : "Koyu temaya geç");
  btn.addEventListener("click", () => {
    const next = current() === "dark" ? "light" : "dark";
    document.documentElement.dataset.theme = next;
    try {
      localStorage.setItem("theme", next);
    } catch {
      // Gizli sekmede depolama kapalı olabilir; tema yine bu oturum için uygulanır.
    }
    sync();
  });
  systemDark.addEventListener("change", sync);
  sync();
}

function bindEvents() {
  document.addEventListener("click", (event) => {
    const target = event.target.closest("button");
    if (!target) return;

    if (target.classList.contains("project-toggle")) {
      const article = target.closest(".project");
      setExpanded(article, !article.classList.contains("is-open"));
    } else if (target.dataset.kind && target.closest("#filters")) {
      filter.kind = target.dataset.kind;
      applyFilter();
    } else if (target.dataset.tech) {
      showTechFilter(target.dataset.tech);
    } else if ("clearTech" in target.dataset) {
      filter.tech = null;
      applyFilter();
    } else if ("clearFilters" in target.dataset) {
      filter.kind = "all";
      filter.tech = null;
      applyFilter();
    } else if (target.dataset.copy) {
      copyLink(target.dataset.copy);
    } else if (target.id === "expand-all") {
      const open = target.dataset.mode !== "close";
      for (const article of visibleArticles()) setExpanded(article, open);
    }
  });

  // Tarayıcının sayfada bul (Ctrl+F) özelliği kapalı ayrıntıda bir eşleşme bulunca bölümü açar.
  for (const detail of listEl.querySelectorAll(".project-detail")) {
    detail.addEventListener("beforematch", () => setExpanded(detail.closest(".project"), true));
  }

  window.addEventListener("hashchange", openFromHash);
}

listEl.innerHTML = projects.map(projectHtml).join("");
renderFilters();
renderTech();
renderStats();
applyFilter();
setupTheme();
bindEvents();
openFromHash();
