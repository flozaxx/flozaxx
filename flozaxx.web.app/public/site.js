"use strict";

// Tüm sayfaların ortak parçaları: ikonlar, etiketler, sol panel ve telefondaki çekmece menü.

const ICONS = {
  noty: '<svg viewBox="232 232 560 560" class="icon-fill" aria-hidden="true"><path d="M304.11 744 L304.11 289.98 L420.53 289.98 L420.53 345.69 Q437.99 312.43 471.25 296.22 Q504.52 280 547.76 280 Q599.31 280 637.98 301.62 Q676.65 323.24 698.27 361.91 Q719.89 400.57 719.89 452.13 L719.89 744 L568.54 744 L548.59 571.04 A93.13 93.13 0 1 0 475.41 571.04 L455.46 744 Z"/></svg>',
  sticker: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 12V7a3 3 0 0 0-3-3H7a3 3 0 0 0-3 3v10a3 3 0 0 0 3 3h5"/><path d="M20 12a8 8 0 0 1-8 8v-3a5 5 0 0 1 5-5z"/><path d="M8.5 13.5c.9.9 2 1.4 3.2 1.5"/><path d="M9 9.2v.6M15 9.2v.6"/></svg>',
  cube: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2.8 20 7.4v9.2l-8 4.6-8-4.6V7.4z"/><path d="m4 7.4 8 4.6 8-4.6M12 12v9.2"/><path d="m8 5.1 8 4.6M16 5.1 8 9.7"/></svg>',
  compass: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="m15.6 8.4-2.1 5.1-5.1 2.1 2.1-5.1z"/></svg>',
  power: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3.5v7.5"/><path d="M6.6 6.8a7.5 7.5 0 1 0 10.8 0"/></svg>',
  globe: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="M3.5 12h17M12 3.5c2.4 2.5 3.5 5.3 3.5 8.5s-1.1 6-3.5 8.5c-2.4-2.5-3.5-5.3-3.5-8.5s1.1-6 3.5-8.5z"/></svg>',
};

const UI_ICONS = {
  external: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M14 4h6v6M20 4l-9 9M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5"/></svg>',
  arrow: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14M13 6l6 6-6 6"/></svg>',
  download: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 4v11M7 10l5 5 5-5M5 20h14"/></svg>',
  back: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5M11 6l-6 6 6 6"/></svg>',
  close: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 7l10 10M17 7 7 17"/></svg>',
};

const KINDS = { app: "Uygulamalar", tool: "Rehber ve araçlar", web: "Web" };
const KIND_LABEL = { app: "Uygulama", tool: "Rehber / araç", web: "Web" };
const STATUS_LABEL = { live: "Yayında", dev: "Geliştiriliyor", idea: "Planlanıyor" };
const RAIL_BADGE = { dev: "yapımda", idea: "plan" };

// Yayında olanlar üstte, geliştirilenler en altta; aynı durumdakiler projects.js sırasını korur.
const STATUS_ORDER = { live: 0, idea: 1, dev: 2 };
const PROJECTS = [...window.PROJECTS].sort((a, b) => STATUS_ORDER[a.status] - STATUS_ORDER[b.status]);
const SITE = window.SITE;

function esc(text) {
  return String(text).replace(/[&<>"']/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
}

function projectUrl(p) {
  return `/${p.slug}`;
}

const DOWNLOADS_REPO = "flozaxx/downloads";

function downloadUrl(p) {
  return `/${p.slug}/dllatest`;
}

function projectBySlug(slug) {
  return PROJECTS.find((p) => p.slug === slug);
}

function tileHtml(p, size = "") {
  const glyph = ICONS[p.icon] || `<span class="tile-letter">${esc(p.name.charAt(0))}</span>`;
  return `<span class="tile ${size}" style="--tile:${esc(p.color || "var(--accent)")};--tile-ink:${esc(p.ink || "#fff")}">${glyph}</span>`;
}

function statusHtml(p) {
  return `<span class="status status-${p.status}">${STATUS_LABEL[p.status]}</span>`;
}

function prefersReducedMotion() {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

/*
  Sol panel. sections: [{ label, href, spy? }]; current: aktif proje slug'ı; currentSub: alt sayfa ("changelog").
  Geniş ekranda sabit sütun, dar ekranda sol üstteki düğmeyle açılan çekmece.
*/
function renderRail({ sections, current = null, currentSub = null }) {
  const sectionLinks = sections
    .map((s) => `<li><a href="${esc(s.href)}"${s.spy ? ` data-spy="${esc(s.spy)}"` : ""}>${esc(s.label)}</a></li>`)
    .join("");

  const projectLinks = PROJECTS.map((p) => {
    const isCurrent = p.slug === current && !currentSub;
    const badge = RAIL_BADGE[p.status] ? `<span class="rail-badge rail-badge-${p.status}">${RAIL_BADGE[p.status]}</span>` : "";
    const inProject = p.slug === current;
    const subs = inProject && p.changelog?.length
      ? `<ul class="rail-sub"><li><a href="${projectUrl(p)}/changelog"${p.slug === current && currentSub === "changelog" ? ' class="is-current" aria-current="page"' : ""}>Changelog</a></li></ul>`
      : "";
    return `<li${inProject ? ' class="is-group"' : ""}><a href="${projectUrl(p)}"${isCurrent ? ' class="is-current" aria-current="page"' : ""}><span class="rail-dot" style="--dot:${esc(p.color)}"></span>${esc(p.name)}${badge}</a>${subs}</li>`;
  }).join("");

  document.getElementById("rail-left").innerHTML = `
    <div class="rail-inner">
      <p class="rail-title">Hızlı erişim</p>
      <ul class="rail-sections">${sectionLinks}</ul>
      <p class="rail-title">Projeler</p>
      <ul class="rail-projects">${projectLinks}</ul>
    </div>`;
}

function setupDrawer() {
  const btn = document.getElementById("menu-toggle");
  const rail = document.getElementById("rail-left");
  const backdrop = document.getElementById("drawer-backdrop");
  const wide = window.matchMedia("(min-width: 1280px)");

  const setOpen = (open, { returnFocus = false } = {}) => {
    document.body.classList.toggle("drawer-open", open);
    btn.setAttribute("aria-expanded", String(open));
    btn.setAttribute("aria-label", open ? "Menüyü kapat" : "Menüyü aç");
    backdrop.hidden = !open;
    if (open) rail.querySelector("a")?.focus();
    else if (returnFocus) btn.focus();
  };

  btn.addEventListener("click", () => setOpen(!document.body.classList.contains("drawer-open")));
  backdrop.addEventListener("click", () => setOpen(false));
  rail.addEventListener("click", (event) => {
    if (event.target.closest("a") && !wide.matches) setOpen(false);
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && document.body.classList.contains("drawer-open")) setOpen(false, { returnFocus: true });
  });
  wide.addEventListener("change", () => setOpen(false));
}

// Kaydırınca, okuma çizgisini geçen son bağlantı hedefini sol panelde vurgular.
function setupScrollSpy(selector, line = () => window.innerHeight * 0.35) {
  const links = [...document.querySelectorAll(selector)];
  const targets = links.map((a) => document.getElementById(a.dataset.spy));
  if (!targets.length || targets.some((t) => !t)) return;
  let frame = 0;
  const update = () => {
    frame = 0;
    let active = 0;
    targets.forEach((el, i) => {
      if (el.getBoundingClientRect().top < line()) active = i;
    });
    // Sayfa son bölüme kadar kaydırılamayabilir; en alttaysak son bağlantı vurgulanır.
    if (window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 2) active = targets.length - 1;
    links.forEach((a, i) => a.classList.toggle("is-active", i === active));
  };
  window.addEventListener("scroll", () => {
    if (!frame) frame = requestAnimationFrame(update);
  }, { passive: true });
  update();
}
