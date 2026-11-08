"use strict";

// Proje sayfaları: /<slug> projenin ana sayfası, /<slug>/changelog sürüm geçmişi.
// Firebase Hosting bu adreslerin hepsini page.html'e yönlendirir; içerik burada çizilir.

const LATEST_RELEASES = 3;
const [slug, sub] = location.pathname.split("/").filter(Boolean);
const project = projectBySlug(slug);
const main = document.getElementById("icerik");
const side = document.getElementById("page-side");

function setMeta(title, description) {
  document.title = `${title} · flozaxx`;
  document.querySelector('meta[name="description"]').setAttribute("content", description);
  document.querySelector('meta[property="og:title"]').setAttribute("content", title);
  document.querySelector('meta[property="og:description"]').setAttribute("content", description);
}

function listHtml(items, cls = "") {
  return `<ul class="feature-list ${cls}">${items.map((f) => `<li>${esc(f)}</li>`).join("")}</ul>`;
}

function linkButtonsHtml(p) {
  return p.links
    .map((link, i) => `<a class="btn${i === 0 ? " btn-primary" : ""}" href="${esc(link.url)}" target="_blank" rel="noopener">${esc(link.label)}${UI_ICONS.external}</a>`)
    .join("");
}

function factsCardHtml(p) {
  const rows = [
    ["Tür", esc(KIND_LABEL[p.kind])],
    ["Durum", statusHtml(p)],
    ["Platform", esc(p.platforms.join(", "))],
  ];
  if (p.tech.length) rows.push(["Teknoloji", p.tech.map((t) => `<a class="tag" href="/?tech=${encodeURIComponent(t)}#projeler">${esc(t)}</a>`).join("")]);
  if (p.version) rows.push(["Sürüm", `<span class="mono">${esc(p.version)}</span>`]);
  if (p.started) rows.push(["Başlangıç", esc(p.started)]);
  if (p.security) rows.push(["Güvenlik", esc(p.security)]);
  return `
    <section class="side-card">
      <h2 class="side-title">Bilgiler</h2>
      <dl class="side-facts">${rows.map(([k, v]) => `<div><dt>${k}</dt><dd>${v}</dd></div>`).join("")}</dl>
    </section>`;
}

function releaseId(r) {
  return `v-${(r.version || r.date).toLowerCase().replace(/[^0-9a-zçğıöşü]+/g, "-")}`;
}

function releaseHtml(r, { full }) {
  const version = r.version ? `<span class="release-version">${esc(r.version)}</span>` : "";
  const title = r.title ? `<p class="release-title">${esc(r.title)}</p>` : "";
  const items = full || !r.title ? listHtml(r.items) : "";
  return `
    <li class="release" id="${releaseId(r)}">
      <div class="release-head">${version}<time>${esc(r.date)}</time></div>
      ${title}
      ${items}
    </li>`;
}

function projectView(p) {
  setMeta(p.name, `${p.name}: ${p.tagline}. ${p.summary}`);

  const planned = p.planned?.length ? `<h2>Planlananlar</h2>${listHtml(p.planned, "feature-list-planned")}` : "";
  const resources = p.resources?.length
    ? `<h2>Ek kaynaklar</h2><div class="resources">${p.resources.map((r) => `
        <a class="resource" href="${esc(r.url)}" target="_blank" rel="noopener">
          <span class="resource-name">${esc(r.name)}${UI_ICONS.external}</span>
          <span class="resource-text">${esc(r.text)}</span>
        </a>`).join("")}</div>`
    : "";
  const releases = p.changelog?.length
    ? `<h2>Son sürümler</h2>
       <ol class="release-list">${p.changelog.slice(0, LATEST_RELEASES).map((r) => releaseHtml(r, { full: false })).join("")}</ol>
       <a class="text-link" href="${projectUrl(p)}/changelog">Tüm sürüm geçmişi${UI_ICONS.arrow}</a>`
    : "";
  const featuresTitle = p.status === "idea" ? "Planlananlar" : "Öne çıkanlar";

  main.innerHTML = `
    <header class="page-head">
      ${tileHtml(p, "tile-lg")}
      <div>
        <h1>${esc(p.name)}</h1>
        <p class="page-tagline">${esc(p.tagline)}</p>
        <p class="page-meta">${statusHtml(p)}<span>${esc(p.platforms.join(" · "))}</span></p>
      </div>
    </header>
    ${p.links.length ? `<div class="page-actions">${linkButtonsHtml(p)}</div>` : ""}
    <p class="page-summary">${esc(p.summary)}</p>
    <h2>${featuresTitle}</h2>
    ${listHtml(p.features)}
    ${planned}
    ${resources}
    ${releases}`;

  side.innerHTML = `<div class="rail-inner">${factsCardHtml(p)}</div>`;
}

function changelogView(p) {
  setMeta(`${p.name} sürüm geçmişi`, `${p.name}: tüm sürümler ve her sürümde neyin değiştiği.`);
  main.innerHTML = `
    <a class="back-link" href="${projectUrl(p)}">${UI_ICONS.back}${esc(p.name)}</a>
    <header class="page-head page-head-plain">
      <div>
        <h1>Changelog</h1>
        <p class="page-tagline">${esc(p.name)} sürüm geçmişi; en yeni en üstte.</p>
      </div>
    </header>
    <ol class="release-list release-list-full">${p.changelog.map((r) => releaseHtml(r, { full: true })).join("")}</ol>`;

  const jump = p.changelog
    .map((r) => `<li><a href="#${releaseId(r)}" data-spy="${releaseId(r)}"><span class="mono">${esc(r.version || r.date)}</span>${esc(r.title || "")}</a></li>`)
    .join("");
  side.innerHTML = `
    <div class="rail-inner">
      <section class="side-card">
        <h2 class="side-title">Sürümler</h2>
        <ul class="version-jump">${jump}</ul>
      </section>
    </div>`;
  setupScrollSpy(".version-jump [data-spy]", () => 140);
}

function notFoundView() {
  setMeta("Sayfa bulunamadı", "Aradığın sayfa burada değil.");
  main.innerHTML = `
    <header class="page-head page-head-plain">
      <div>
        <h1>Sayfa bulunamadı</h1>
        <p class="page-tagline">Adres değişmiş ya da yanlış yazılmış olabilir.</p>
      </div>
    </header>
    <div class="page-actions"><a class="btn btn-primary" href="/">Tüm projeler</a></div>`;
  side.hidden = true;
}

renderRail({
  sections: [
    { label: "Ana sayfa", href: "/" },
    { label: "Projeler", href: "/#projeler" },
    { label: "Bağlantılar", href: "/#baglantilar" },
  ],
  current: project?.slug,
  currentSub: sub,
});

if (!project || (sub && !(sub === "changelog" && project.changelog?.length))) notFoundView();
else if (sub === "changelog") changelogView(project);
else projectView(project);

document.getElementById("updated").textContent = `Son güncelleme: ${SITE.updated}`;
setupDrawer();
