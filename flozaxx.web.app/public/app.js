"use strict";

// Ana sayfa: sade proje kartları, süzgeçler, teknoloji listesi ve sağ sütun.

const SIDE_UPDATE_COUNT = 4;
const filter = { kind: "all", tech: null };
const $ = (id) => document.getElementById(id);
const listEl = $("project-list");

function projectCardHtml(p) {
  return `
    <a class="project" id="kart-${esc(p.slug)}" href="${projectUrl(p)}" data-kind="${esc(p.kind)}">
      ${tileHtml(p)}
      <span class="project-text">
        <span class="project-title">${esc(p.name)}${statusHtml(p)}</span>
        <span class="project-tagline">${esc(p.tagline)}</span>
        <span class="project-meta">${esc(p.platforms.join(" · "))}</span>
      </span>
      <span class="go">Projeye git${UI_ICONS.arrow}</span>
    </a>`;
}

function projectRef(slug) {
  const p = PROJECTS.find((x) => x.id === slug || x.slug === slug);
  return p ? `<a href="${projectUrl(p)}" class="side-project"><span class="rail-dot" style="--dot:${esc(p.color)}"></span>${esc(p.name)}</a>` : "";
}

function renderSide() {
  $("now-list").innerHTML = SITE.now.map((n) => `<li>${projectRef(n.project)}<p>${esc(n.text)}</p></li>`).join("");
  $("update-list").innerHTML = SITE.updates
    .slice(0, SIDE_UPDATE_COUNT)
    .map((u) => `<li><time>${esc(u.date)}</time><p>${esc(u.text)}</p>${projectRef(u.project)}</li>`)
    .join("");
}

function matches(p) {
  return (filter.kind === "all" || p.kind === filter.kind) && (!filter.tech || p.tech.includes(filter.tech));
}

function renderFilters() {
  const counts = { all: PROJECTS.length };
  for (const p of PROJECTS) counts[p.kind] = (counts[p.kind] || 0) + 1;
  const options = [["all", "Hepsi"], ...Object.entries(KINDS).filter(([key]) => counts[key])];
  $("filters").innerHTML = options
    .map(([key, label]) => `<button type="button" class="chip" data-kind="${key}" aria-pressed="false">${label}<span class="chip-count">${counts[key]}</span></button>`)
    .join("");
}

function applyFilter() {
  let shown = 0;
  for (const p of PROJECTS) {
    const ok = matches(p);
    $(`kart-${p.slug}`).hidden = !ok;
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
}

function renderTech() {
  const counts = new Map();
  for (const p of PROJECTS) for (const t of p.tech) counts.set(t, (counts.get(t) || 0) + 1);
  const sorted = [...counts].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0], "tr"));
  $("tech-list").innerHTML = sorted
    .map(([t, n]) => `<button type="button" class="chip" data-tech="${esc(t)}" aria-pressed="false">${esc(t)}<span class="chip-count">${n}</span></button>`)
    .join("");
}

function renderStats() {
  const count = (status) => PROJECTS.filter((p) => p.status === status).length;
  const parts = [`${PROJECTS.length} proje`, `${count("live")} yayında`];
  if (count("dev")) parts.push(`${count("dev")} geliştiriliyor`);
  if (count("idea")) parts.push(`${count("idea")} planlanıyor`);
  $("hero-stats").textContent = parts.join(" · ");
  $("updated").textContent = `Son güncelleme: ${SITE.updated}`;
}

function showTechFilter(tech, { scroll = true } = {}) {
  filter.tech = filter.tech === tech ? null : tech;
  filter.kind = "all";
  applyFilter();
  if (scroll) $("projeler").scrollIntoView({ behavior: prefersReducedMotion() ? "auto" : "smooth", block: "start" });
}

// Compat: eski paylaşılan bağlantılar (#noty gibi) projenin kendi sayfasına gider.
function redirectOldHash() {
  const id = decodeURIComponent(location.hash.slice(1));
  const p = PROJECTS.find((x) => x.id === id || x.slug === id);
  if (p) location.replace(projectUrl(p));
  else if (id === "araclar") $("araclar").open = true;
}

function bindEvents() {
  document.addEventListener("click", (event) => {
    const target = event.target.closest("button");
    if (!target) return;
    if (target.dataset.kind && target.closest("#filters")) {
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
    }
  });
  window.addEventListener("hashchange", redirectOldHash);
}

renderRail({
  sections: [
    { label: "Giriş", href: "#top", spy: "top" },
    { label: "Projeler", href: "#projeler", spy: "projeler" },
    { label: "Teknolojiler", href: "#teknolojiler", spy: "teknolojiler" },
    { label: "Bağlantılar", href: "#baglantilar", spy: "baglantilar" },
  ],
});
listEl.innerHTML = PROJECTS.map(projectCardHtml).join("");
renderSide();
renderFilters();
renderTech();
renderStats();
applyFilter();
bindEvents();
setupDrawer();
setupScrollSpy(".rail-sections [data-spy]");
redirectOldHash();

const techParam = new URLSearchParams(location.search).get("tech");
if (techParam && PROJECTS.some((p) => p.tech.includes(techParam))) showTechFilter(techParam, { scroll: false });
