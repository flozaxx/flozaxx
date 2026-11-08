"use strict";

// Proje sayfaları: /<slug> projenin ana sayfası, /<slug>/changelog sürüm geçmişi.
// Firebase Hosting bu adreslerin hepsini page.html'e yönlendirir; içerik burada çizilir.

const LATEST_RELEASES = 3;
const [slug, sub] = location.pathname.split("/").filter(Boolean);
let project;
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
  const download = p.download
    ? `<a class="btn btn-primary" href="${downloadUrl(p)}">${UI_ICONS.download}İndir<span class="btn-note">${esc(p.download.platform)}</span></a>`
    : "";
  const links = p.links
    .map((link, i) => `<a class="btn${!download && i === 0 ? " btn-primary" : ""}" href="${esc(link.url)}" target="_blank" rel="noopener">${esc(link.label)}${UI_ICONS.external}</a>`)
    .join("");
  return download + links;
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
    ${p.links.length || p.download ? `<div class="page-actions">${linkButtonsHtml(p)}</div>` : ""}
    <p class="page-summary">${esc(p.summary)}</p>
    <h2>${featuresTitle}</h2>
    ${listHtml(p.features)}
    ${planned}
    ${resources}
    ${releases}`;

  side.innerHTML = `<div class="rail-inner">${factsCardHtml(p)}</div>`;
  if (p.download) hideDownloadIfMissing(p);
}

// Dosya henüz yüklenmediyse "İndir" düğmesi GitHub'da 404'e gideceği için gizlenir.
async function hideDownloadIfMissing(p) {
  const btn = main.querySelector(`a[href="${downloadUrl(p)}"]`);
  try {
    const res = await fetch(`https://api.github.com/repos/${DOWNLOADS_REPO}/releases/tags/${encodeURIComponent(p.slug)}`);
    // Yalnızca GitHub "yok" derse gizlenir; istek sınırı gibi hatalarda düğme kalır, bağlantı yine çalışır.
    const missing = res.status === 404 || (res.ok && !(await res.json()).assets.some((a) => a.name === p.download.file));
    if (missing) btn.remove();
  } catch {
    // Ağ hatası: düğme kalır.
  }
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

function formatSize(bytes) {
  return bytes >= 1e6 ? `${(bytes / 1e6).toFixed(1).replace(".", ",")} MB` : `${Math.ceil(bytes / 1e3)} KB`;
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString("tr-TR", { day: "numeric", month: "long", year: "numeric" });
}

// Sürüm, boyut, tarih ve SHA-256 GitHub'daki release'ten okunur; böylece yeni dosya yüklemek sayfayı da günceller.
async function fillDownloadCard(p) {
  const card = document.getElementById(`dl-${p.slug}`);
  const info = card.querySelector(".dl-info");
  let release;
  try {
    const res = await fetch(`https://api.github.com/repos/${DOWNLOADS_REPO}/releases/tags/${encodeURIComponent(p.slug)}`);
    if (res.status === 404) release = { assets: [] };
    else if (res.ok) release = await res.json();
  } catch {
    // Ağ hatası: aşağıda bilgisiz kart gösterilir.
  }
  // GitHub'a ulaşılamadıysa (ör. saatlik istek sınırı) dosyanın varlığı bilinemez; düğme kalır.
  if (!release) {
    info.textContent = "Dosya bilgileri şu an alınamadı; indirme bağlantısı çalışıyor.";
    return;
  }
  const asset = release.assets.find((a) => a.name === p.download.file);
  if (asset) {
    const sha = asset.digest?.startsWith("sha256:") ? asset.digest.slice(7) : null;
    info.innerHTML = `
      <span>${esc(release.name || p.slug)}</span>
      <span>${formatSize(asset.size)}</span>
      <span>${formatDate(asset.updated_at)}</span>`;
    if (sha) {
      card.querySelector(".dl-sha").innerHTML = `<span class="dl-sha-label">SHA-256</span><code>${esc(sha)}</code><button type="button" class="text-btn" data-copy-sha="${esc(sha)}">Kopyala</button>`;
    }
    return;
  }
  card.classList.add("is-empty");
  info.textContent = "Henüz yüklenmiş bir dosya yok.";
  card.querySelector(".btn").remove();
}

function downloadView() {
  setMeta("İndirmeler", "flozaxx projelerinin indirilebilir dosyaları; her bağlantı doğrudan son sürümü indirir.");
  const items = PROJECTS.filter((p) => p.download);
  main.innerHTML = `
    <header class="page-head page-head-plain">
      <div>
        <h1>İndirmeler</h1>
        <p class="page-tagline">Düğmeye basınca dosya doğrudan iner; her zaman son sürüm.</p>
      </div>
    </header>
    <div class="dl-list">${items.map((p) => `
      <article class="dl-card" id="dl-${esc(p.slug)}">
        ${tileHtml(p)}
        <div class="dl-body">
          <h2><a href="${projectUrl(p)}">${esc(p.name)}</a>${statusHtml(p)}</h2>
          <p class="dl-info">Bilgiler yükleniyor…</p>
          <p class="dl-sha"></p>
        </div>
        <a class="btn btn-primary" href="${downloadUrl(p)}">${UI_ICONS.download}İndir<span class="btn-note">${esc(p.download.platform)}</span></a>
      </article>`).join("")}
    </div>
    <p class="dl-note">Android, Play Store dışından indirilen uygulamalar için “bilinmeyen kaynak” izni ister. Kurulumdan önce SHA-256 kodunu karşılaştırarak dosyanın değişmediğini doğrulayabilirsin. Kalıcı bağlantı: <code>flozaxx.com/&lt;proje&gt;/dllatest</code></p>`;
  side.hidden = true;
  items.forEach(fillDownloadCard);
  main.addEventListener("click", async (event) => {
    const btn = event.target.closest("[data-copy-sha]");
    if (!btn) return;
    await navigator.clipboard.writeText(btn.dataset.copySha);
    btn.textContent = "Kopyalandı";
  });
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

contentReady.then(() => {
  project = projectBySlug(slug);
  renderRail({
    sections: [
      { label: "Ana sayfa", href: "/" },
      { label: "Projeler", href: "/#projeler" },
      { label: "İndirmeler", href: "/download" },
    ],
    current: slug === "download" ? null : project?.slug,
    currentSub: sub,
  });

  if (slug === "download" && !sub) downloadView();
  else if (!project || (sub && !(sub === "changelog" && project.changelog?.length))) notFoundView();
  else if (sub === "changelog") changelogView(project);
  else projectView(project);

  document.getElementById("updated").textContent = `Son güncelleme: ${SITE.updated}`;
  setupDrawer();
});
