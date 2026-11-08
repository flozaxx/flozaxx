// flozaxx yönetim paneli: siteyi besleyen site/content belgesini, sitenin kendi görünümüne yakın bir düzende düzenler.
// Yalnızca sahibin yazabilmesini Firestore kuralları sağlar; bu sayfa yalnızca arayüzdür.
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import { getAuth, GoogleAuthProvider, signInWithPopup, onAuthStateChanged, signOut } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";
import { getFirestore, doc, getDoc, setDoc, updateDoc, deleteDoc, collection, getDocs, query, orderBy, limit, increment, serverTimestamp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";

const RESERVED_SLUGS = new Set(["admin", "download", "downloads", "p", "favicon", "page", "index", "404"]);
const KIND_OPTIONS = { app: "Uygulama", tool: "Rehber / araç", web: "Web" };
const STATUS_OPTIONS = { live: "Yayında", dev: "Geliştiriliyor", idea: "Planlanıyor" };
const OPTIONAL_KEYS = new Set(["ink", "version", "started", "security", "title"]);

const main = document.getElementById("admin");
const side = document.getElementById("admin-side");
const rail = document.getElementById("rail-left");
const savebar = document.getElementById("savebar");
let auth;
let db;
let saved = null;
let draft = null;
let view = { name: "project", index: 0 };
let admins = null;
let attempts = [];
let drawerReady = false;

const clone = (value) => JSON.parse(JSON.stringify(value));
const isDirty = () => JSON.stringify(saved) !== JSON.stringify(draft);

function toast(message) {
  const el = document.getElementById("toast");
  el.textContent = message;
  el.classList.add("is-visible");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => el.classList.remove("is-visible"), 2400);
}

function getPath(path) {
  return path.split(".").reduce((obj, key) => obj?.[key], draft);
}

function setPath(path, value) {
  const keys = path.split(".");
  const last = keys.pop();
  const parent = keys.reduce((obj, key) => obj[key], draft);
  if (value === undefined) delete parent[last];
  else parent[last] = value;
}

/* ---------- Küçük bileşenler ---------- */

const text = (path, { cls = "", placeholder = "", type = "text", label = "" } = {}) => {
  const value = getPath(path);
  const shown = type === "csv" ? (value || []).join(", ") : value ?? "";
  return `<input class="ghost ${cls}" data-path="${path}" data-type="${type}" value="${esc(shown)}" placeholder="${esc(placeholder)}"${label ? ` aria-label="${esc(label)}"` : ""}>`;
};

const area = (path, { cls = "", placeholder = "" } = {}) =>
  `<textarea class="ghost ${cls}" data-path="${path}" data-type="text" rows="3" placeholder="${esc(placeholder)}">${esc(getPath(path) || "")}</textarea>`;

const select = (path, options, { cls = "", label = "" } = {}) => {
  const value = getPath(path) ?? "";
  return `<select class="ghost-select ${cls}" data-path="${path}" data-type="text"${label ? ` aria-label="${esc(label)}"` : ""}>${Object.entries(options)
    .map(([k, v]) => `<option value="${esc(k)}"${k === value ? " selected" : ""}>${esc(v)}</option>`).join("")}</select>`;
};

const removeBtn = (listPath, i) => `<button type="button" class="mini mini-danger" data-remove="${listPath}" data-index="${i}" aria-label="Kaldır">✕</button>`;
const moveBtns = (listPath, i, length) => `
  <button type="button" class="mini" data-move="${listPath}" data-index="${i}" data-dir="-1" ${i === 0 ? "disabled" : ""} aria-label="Yukarı">↑</button>
  <button type="button" class="mini" data-move="${listPath}" data-index="${i}" data-dir="1" ${i === length - 1 ? "disabled" : ""} aria-label="Aşağı">↓</button>`;
const addBtn = (listPath, label, template) => `<button type="button" class="add-btn" data-add="${listPath}" data-template='${esc(JSON.stringify(template))}'>+ ${label}</button>`;

function stringList(listPath, placeholder) {
  const list = getPath(listPath) || [];
  const items = list.map((_, i) => `<li>${text(`${listPath}.${i}`, { placeholder })}${removeBtn(listPath, i)}</li>`).join("");
  return `<ul class="feature-list edit-list">${items}</ul>${addBtn(listPath, "Madde ekle", "")}`;
}

function sideField(label, control) {
  return `<div><dt>${label}</dt><dd>${control}</dd></div>`;
}

/* ---------- Sol menü ---------- */

function renderRail() {
  const projects = draft.projects.map((p, i) => {
    const current = view.name === "project" && view.index === i;
    const badge = p.status === "dev" ? RAIL_BADGE.dev : p.status === "idea" ? RAIL_BADGE.idea : "";
    return `<li${current ? ' class="is-group"' : ""}><a href="#" data-view="project" data-index="${i}"${current ? ' class="is-current"' : ""}><span class="rail-dot" style="--dot:${esc(p.color || "#4a5462")}"></span>${esc(p.name || "Adsız proje")}${badge}</a></li>`;
  }).join("");
  const fresh = attempts.filter((a) => !a.seen).length;
  const link = (name, label, badge = "") => `<li><a href="#" data-view="${name}"${view.name === name ? ' class="is-current"' : ""}>${label}${badge}</a></li>`;
  rail.innerHTML = `
    <div class="rail-inner">
      <p class="rail-title">Yönetim</p>
      <ul class="rail-sections">
        ${link("home", "Ana sayfa içeriği")}
        ${link("access", "Erişim", fresh ? `<span class="rail-count" title="Yeni başarısız giriş">${fresh}</span>` : "")}
        ${link("setup", "Kurulum")}
        <li><a href="https://flozaxx.com" target="_blank" rel="noopener">Siteyi aç ↗</a></li>
      </ul>
      <p class="rail-title">Projeler</p>
      <ul class="rail-projects">${projects}</ul>
      <button type="button" class="add-btn" id="add-project">+ Yeni proje</button>
    </div>`;
}

/* ---------- Proje sayfası ---------- */

function projectMain(i) {
  const p = draft.projects[i];
  const base = `projects.${i}`;
  const links = (p.links || []).map((_, j) => `
    <span class="link-edit">${text(`${base}.links.${j}.label`, { placeholder: "Düğme yazısı", label: "Düğme yazısı" })}${text(`${base}.links.${j}.url`, { cls: "mono-input", placeholder: "https://", label: "Adres" })}${removeBtn(`${base}.links`, j)}</span>`).join("");
  const resources = (p.resources || []).map((_, j) => `
    <div class="resource edit-card">
      <div class="edit-card-head">${text(`${base}.resources.${j}.name`, { cls: "strong", placeholder: "Kaynak adı" })}${removeBtn(`${base}.resources`, j)}</div>
      ${text(`${base}.resources.${j}.url`, { cls: "mono-input", placeholder: "https://" })}
      ${area(`${base}.resources.${j}.text`, { placeholder: "Kısa açıklama" })}
    </div>`).join("");
  const changelog = (p.changelog || []).map((_, j, all) => `
    <li class="release edit-card">
      <div class="release-head">
        ${text(`${base}.changelog.${j}.version`, { cls: "release-version-input", placeholder: "1.0.0" })}
        ${text(`${base}.changelog.${j}.date`, { cls: "small", placeholder: "Tarih" })}
        <span class="edit-card-tools">${moveBtns(`${base}.changelog`, j, all.length)}${removeBtn(`${base}.changelog`, j)}</span>
      </div>
      ${text(`${base}.changelog.${j}.title`, { cls: "strong", placeholder: "Başlık" })}
      ${stringList(`${base}.changelog.${j}.items`, "Değişiklik")}
    </li>`).join("");

  return `
    <div class="edit-toolbar">
      <span class="muted">Sıra ${i + 1}/${draft.projects.length}</span>
      ${moveBtns("projects", i, draft.projects.length)}
      <a class="text-link" href="https://flozaxx.com/${esc(p.slug)}" target="_blank" rel="noopener">Sitede aç${UI_ICONS.external}</a>
      <button type="button" class="text-btn danger-text" data-delete-project="${i}">Projeyi sil</button>
    </div>
    <header class="page-head">
      ${tileHtml(p, "tile-lg")}
      <div class="page-head-text">
        ${text(`${base}.name`, { cls: "h1-input", placeholder: "Proje adı", label: "Proje adı" })}
        ${text(`${base}.tagline`, { cls: "page-tagline", placeholder: "Kısa açıklama", label: "Kısa açıklama" })}
        <p class="page-meta">${select(`${base}.status`, STATUS_OPTIONS, { cls: `status status-${p.status}`, label: "Durum" })}${text(`${base}.platforms`, { type: "csv", cls: "mono-input", placeholder: "Web, Android", label: "Platformlar" })}</p>
      </div>
    </header>
    <div class="page-actions links-edit">${links}${addBtn(`${base}.links`, "Bağlantı düğmesi", { label: "", url: "https://" })}</div>
    ${area(`${base}.summary`, { cls: "page-summary", placeholder: "Proje ne işe yarıyor?" })}
    <h2>${p.status === "idea" ? "Planlananlar" : "Öne çıkanlar"}</h2>
    ${stringList(`${base}.features`, "Madde")}
    ${p.status === "idea" ? "" : `<h2>Planlananlar</h2>${stringList(`${base}.planned`, "Planlanan özellik")}`}
    <h2>Ek kaynaklar</h2>
    <div class="resources">${resources}</div>
    ${addBtn(`${base}.resources`, "Kaynak ekle", { name: "", url: "https://", text: "" })}
    <h2>Sürüm geçmişi</h2>
    <ol class="release-list">${changelog}</ol>
    ${addBtn(`${base}.changelog`, "Sürüm ekle", { version: "", date: "", title: "", items: [] })}`;
}

function projectSide(i) {
  const p = draft.projects[i];
  const base = `projects.${i}`;
  const iconOptions = Object.fromEntries([["", "Baş harf"], ...Object.keys(ICONS).map((k) => [k, k])]);
  const solid = /^#[0-9a-f]{6}$/i.test(p.color || "") ? p.color : "#4a5462";
  const download = p.download
    ? `${sideField("Platform", text(`${base}.download.platform`, { placeholder: "Android" }))}
       <p class="field-hint">Dosyayı <a href="https://github.com/flozaxx/downloads/releases" target="_blank" rel="noopener">downloads</a> deposunda <b>${esc(p.slug)}</b> etiketli release'e <b>${esc(p.slug)}.apk</b> adıyla yükle.</p>`
    : "";
  return `
    <div class="rail-inner">
      <section class="side-card">
        <h2 class="side-title">Bilgiler</h2>
        <dl class="side-facts">
          ${sideField("Adres", `<span class="slug-row">flozaxx.com/${text(`${base}.slug`, { cls: "mono-input", label: "Adres" })}</span>`)}
          ${sideField("Tür", select(`${base}.kind`, KIND_OPTIONS))}
          ${sideField("İkon", select(`${base}.icon`, iconOptions))}
          ${sideField("İkon zemini", `<span class="color-row"><input type="color" value="${solid}" data-path="${base}.color" data-type="text" aria-label="Renk seç">${text(`${base}.color`, { cls: "mono-input color-text" })}</span>`)}
          ${sideField("İkon rengi", text(`${base}.ink`, { cls: "mono-input", placeholder: "#fff" }))}
          ${sideField("Teknoloji", text(`${base}.tech`, { type: "csv", placeholder: "Flutter, Firebase" }))}
          ${sideField("Sürüm", text(`${base}.version`, { cls: "mono-input", placeholder: "örn. 1.0.0" }))}
          ${sideField("Başlangıç", text(`${base}.started`, { placeholder: "Ekim 2026" }))}
          ${sideField("Güvenlik", text(`${base}.security`, { placeholder: "isteğe bağlı" }))}
        </dl>
      </section>
      <section class="side-card">
        <h2 class="side-title">İndirme</h2>
        <label class="check"><input type="checkbox" data-download="${i}" ${p.download ? "checked" : ""}> İndirilebilir dosya var</label>
        <dl class="side-facts">${download}</dl>
      </section>
    </div>`;
}

/* ---------- Erişim ve kurallar ---------- */

const ADMINS_PATH = ["site", "admins"];

// Sahip (owner) kurallara kimliğiyle yazılır; listeyi boşaltsa bile dışarıda kalmaz.
function rulesText(ownerUid) {
  return `rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isAdmin() {
      return request.auth != null && (
        request.auth.uid == "${ownerUid}" || (
          request.auth.token.email_verified == true &&
          exists(/databases/$(database)/documents/site/admins) &&
          request.auth.token.email.lower() in get(/databases/$(database)/documents/site/admins).data.emails
        )
      );
    }
    match /site/content {
      allow read: if true;
      allow write: if isAdmin();
    }
    match /site/admins {
      allow read, write: if isAdmin();
    }
    // Başarısız giriş kaydı: her hesap yalnızca kendi kaydını yazabilir, bilgileri değiştiremez; okumak yöneticilere.
    match /loginAttempts/{uid} {
      allow create, update: if request.auth != null && request.auth.uid == uid
        && request.resource.data.keys().hasOnly(["uid", "email", "name", "photo", "agent", "lang", "count", "at", "seen"])
        && request.resource.data.uid == uid
        && request.resource.data.email == request.auth.token.email
        && request.resource.data.seen == false
        && request.resource.data.at == request.time
        && request.resource.data.count == (resource == null ? 1 : resource.data.count + 1);
      allow read, delete: if isAdmin();
      allow update: if isAdmin() && request.resource.data.diff(resource.data).affectedKeys().hasOnly(["seen"]);
    }
  }
}`;
}

function rulesBlock(ownerUid) {
  return `
    <ol class="setup-steps">
      <li><a href="https://console.firebase.google.com/project/flozaxx-a8521/firestore/rules" target="_blank" rel="noopener">Firebase konsolu → Firestore → Kurallar</a> sayfasını aç.</li>
      <li>Metnin tamamını silip aşağıdakini yapıştır, <b>Yayınla</b>'ya bas.</li>
    </ol>
    <div class="rules-box"><pre id="rules-text">${esc(rulesText(ownerUid))}</pre><button type="button" class="btn btn-sm" id="copy-rules">Kopyala</button></div>`;
}

function accessMain() {
  const me = auth.currentUser.email.toLowerCase();
  const rows = admins.emails.map((email, i) => `
    <li class="edit-card access-row">
      <span>${esc(email)}${email === me ? ' <span class="muted">(sen)</span>' : ""}</span>
      <button type="button" class="mini mini-danger" data-remove-admin="${i}" aria-label="${esc(email)} erişimini kaldır">✕</button>
    </li>`).join("");
  return `
    <header class="page-head page-head-plain"><div><h1>Erişim</h1><p class="page-tagline">Bu listedeki Google hesapları yönetim paneline girip siteyi düzenleyebilir.</p></div></header>
    <ol class="edit-stack">${rows || '<li class="muted">Listede kimse yok; yalnızca sahip hesap girebilir.</li>'}</ol>
    <form class="access-add" id="add-admin">
      <input class="ghost" type="email" id="admin-email" placeholder="ornek@gmail.com" required aria-label="Eklenecek e-posta">
      <button type="submit" class="btn btn-sm btn-primary">Ekle</button>
    </form>
    <p class="field-hint">Değişiklik hemen kaydedilir. Sahip hesap (panelin ilk kurulduğu hesap) listede olmasa da her zaman girebilir.</p>
    <h2>Başarısız girişler</h2>
    ${attemptsHtml()}`;
}

function describeAgent(agent = "") {
  const os = /Android/.test(agent) ? "Android" : /iPhone|iPad/.test(agent) ? "iOS" : /Windows/.test(agent) ? "Windows" : /Mac OS/.test(agent) ? "macOS" : /Linux/.test(agent) ? "Linux" : "Bilinmeyen sistem";
  const browser = /Edg\//.test(agent) ? "Edge" : /OPR\/|Opera/.test(agent) ? "Opera" : /Firefox\//.test(agent) ? "Firefox" : /Chrome\//.test(agent) ? "Chrome" : /Safari\//.test(agent) ? "Safari" : "tarayıcı";
  return `${browser} · ${os}`;
}

function formatTime(ts) {
  return ts?.toDate ? ts.toDate().toLocaleString("tr-TR", { dateStyle: "medium", timeStyle: "short" }) : "";
}

function attemptsHtml() {
  if (!attempts.length) return '<p class="muted">Yetkisiz giriş denemesi yok.</p>';
  const rows = attempts.map((a) => `
    <li class="edit-card attempt${a.seen ? "" : " is-new"}">
      <div class="attempt-main">
        ${a.photo ? `<img src="${esc(a.photo)}" alt="" width="36" height="36" referrerpolicy="no-referrer">` : '<span class="attempt-avatar"></span>'}
        <div>
          <b>${esc(a.name || "Adsız")}</b>${a.seen ? "" : ' <span class="status status-dev">yeni</span>'}
          <div class="muted small-text">${esc(a.email)}</div>
          <div class="muted small-text">${esc(formatTime(a.at))} · ${a.count} deneme · ${esc(describeAgent(a.agent))}${a.lang ? ` · ${esc(a.lang)}` : ""}</div>
        </div>
      </div>
      <span class="edit-card-tools">
        ${admins.emails.includes(a.email.toLowerCase()) ? '<span class="muted small-text">erişimi var</span>' : `<button type="button" class="btn btn-sm" data-grant="${esc(a.uid)}">Erişim ver</button>`}
        <button type="button" class="mini mini-danger" data-forget="${esc(a.uid)}" aria-label="Kaydı sil">✕</button>
      </span>
    </li>`).join("");
  return `<ol class="edit-stack">${rows}</ol>`;
}

async function loadAttempts() {
  const snap = await getDocs(query(collection(db, "loginAttempts"), orderBy("at", "desc"), limit(50)));
  attempts = snap.docs.map((d) => d.data());
  const fresh = attempts.filter((a) => !a.seen).length;
  if (fresh) toast(`${fresh} yeni başarısız giriş denemesi var`);
}

// Erişim sayfası açılınca yeni denemeler görüldü sayılır; rozet o zaman söner.
async function markAttemptsSeen() {
  const fresh = attempts.filter((a) => !a.seen);
  await Promise.all(fresh.map((a) => updateDoc(doc(db, "loginAttempts", a.uid), { seen: true })));
  fresh.forEach((a) => { a.seen = true; });
}

async function saveAdmins(emails) {
  await setDoc(doc(db, ...ADMINS_PATH), { owner: admins.owner, emails });
  admins.emails = emails;
  toast("Erişim listesi kaydedildi");
  render();
}

document.addEventListener("submit", async (event) => {
  if (event.target.id !== "add-admin") return;
  event.preventDefault();
  const email = document.getElementById("admin-email").value.trim().toLowerCase();
  if (!email || admins.emails.includes(email)) return;
  await saveAdmins([...admins.emails, email]);
});

/* ---------- Ana sayfa ve kurulum ---------- */

function homeMain() {
  const options = Object.fromEntries(draft.projects.map((p) => [p.id, p.name || p.slug]));
  const now = (draft.site.now || []).map((_, i, all) => `
    <li class="edit-card">
      <div class="edit-card-head">${select(`site.now.${i}.project`, options, { label: "Proje" })}<span class="edit-card-tools">${moveBtns("site.now", i, all.length)}${removeBtn("site.now", i)}</span></div>
      ${text(`site.now.${i}.text`, { placeholder: "Ne üzerinde çalışıyorsun?" })}
    </li>`).join("");
  const updates = (draft.site.updates || []).map((_, i, all) => `
    <li class="edit-card">
      <div class="edit-card-head">${text(`site.updates.${i}.date`, { cls: "small", placeholder: "8 Kas 2026" })}${select(`site.updates.${i}.project`, options, { label: "Proje" })}<span class="edit-card-tools">${moveBtns("site.updates", i, all.length)}${removeBtn("site.updates", i)}</span></div>
      ${text(`site.updates.${i}.text`, { placeholder: "Ne değişti?" })}
    </li>`).join("");
  const first = draft.projects[0]?.id || "";
  return `
    <header class="page-head page-head-plain"><div><h1>Ana sayfa içeriği</h1><p class="page-tagline">Sağ sütundaki kutular ve alt bilgi.</p></div></header>
    <h2>Şu an</h2>
    <ol class="edit-stack">${now}</ol>
    ${addBtn("site.now", "Satır ekle", { project: first, text: "" })}
    <h2>Son güncellemeler <small class="muted">en yeni en üstte · sitede ilk 4'ü görünür</small></h2>
    <ol class="edit-stack">${updates}</ol>
    ${addBtn("site.updates", "Güncelleme ekle", { date: "", project: first, text: "" })}
    <h2>Alt bilgi</h2>
    <p class="edit-inline">Son güncelleme: ${text("site.updated", { placeholder: "8 Kasım 2026" })}</p>`;
}

function setupMain() {
  return `
    <header class="page-head page-head-plain"><div><h1>Kurulum</h1><p class="page-tagline">Firestore kuralları; değiştiğinde bir kez yapıştırılır.</p></div></header>
    ${rulesBlock(admins.owner)}
    <p class="field-hint">Herkes site içeriğini okuyabilir; içeriği ve erişim listesini yalnızca sahip hesap ile Erişim sayfasındaki hesaplar değiştirebilir.</p>`;
}

/* ---------- Çizim ---------- */

function render() {
  if (!draft) return;
  if (view.name === "project" && !draft.projects[view.index]) view = draft.projects.length ? { name: "project", index: 0 } : { name: "home" };
  renderRail();
  if (view.name === "project") {
    main.innerHTML = projectMain(view.index);
    side.innerHTML = projectSide(view.index);
    side.hidden = false;
  } else {
    main.innerHTML = view.name === "home" ? homeMain() : view.name === "access" ? accessMain() : setupMain();
    side.hidden = true;
  }
  savebar.hidden = !isDirty();
  if (!drawerReady) {
    setupDrawer();
    drawerReady = true;
  }
}

// Yazarken bütün sayfa yeniden çizilmez (imleç kaybolmasın); yalnızca ad, renk ve ikon gibi görünenler güncellenir.
function refreshLive() {
  if (view.name !== "project") return;
  const p = draft.projects[view.index];
  main.querySelector(".page-head .tile")?.replaceWith(document.createRange().createContextualFragment(tileHtml(p, "tile-lg")));
  renderRail();
  savebar.hidden = !isDirty();
}

/* ---------- Olaylar ---------- */

function readInput(el) {
  if (el.dataset.type === "csv") return el.value.split(",").map((s) => s.trim()).filter(Boolean);
  return el.value;
}

document.addEventListener("input", (event) => {
  const el = event.target.closest("[data-path]");
  if (!el || !draft) return;
  const value = readInput(el);
  const key = el.dataset.path.split(".").pop();
  setPath(el.dataset.path, value === "" && OPTIONAL_KEYS.has(key) ? undefined : value);
  if (el.type === "color") document.querySelector(`.color-text[data-path="${el.dataset.path}"]`).value = value;
  if (key === "slug") {
    const p = draft.projects[view.index];
    if (p.download) p.download.file = `${value}.apk`;
  }
  if (key === "status") {
    render();
    return;
  }
  refreshLive();
});

document.addEventListener("change", (event) => {
  const el = event.target;
  if (!el.matches("[data-download]")) return;
  const p = draft.projects[Number(el.dataset.download)];
  if (el.checked) p.download = { file: `${p.slug}.apk`, platform: "Android" };
  else delete p.download;
  render();
});

document.addEventListener("click", async (event) => {
  const target = event.target.closest("a[data-view], button");
  if (!target) return;
  if (target.id === "copy-rules") {
    await navigator.clipboard.writeText(document.getElementById("rules-text").textContent);
    toast("Kurallar kopyalandı");
    return;
  }
  if (!draft) return;
  const { dataset } = target;
  if (dataset.view) {
    event.preventDefault();
    view = dataset.view === "project" ? { name: "project", index: Number(dataset.index) } : { name: dataset.view };
    render();
    if (view.name === "access" && attempts.some((a) => !a.seen)) markAttemptsSeen().then(renderRail);
    window.scrollTo({ top: 0 });
    return;
  }
  if (dataset.move) {
    const list = getPath(dataset.move);
    const i = Number(dataset.index);
    const j = i + Number(dataset.dir);
    [list[i], list[j]] = [list[j], list[i]];
    if (dataset.move === "projects") view.index = j;
  } else if (dataset.remove) {
    getPath(dataset.remove).splice(Number(dataset.index), 1);
  } else if (dataset.add) {
    if (!getPath(dataset.add)) setPath(dataset.add, []);
    getPath(dataset.add).push(JSON.parse(dataset.template));
  } else if (dataset.deleteProject) {
    const p = draft.projects[Number(dataset.deleteProject)];
    if (!confirm(`“${p.name}” silinsin mi? Kaydedene kadar “Vazgeç” ile geri alabilirsin.`)) return;
    draft.projects.splice(Number(dataset.deleteProject), 1);
    view = { name: "project", index: Math.max(0, view.index - 1) };
  } else if (target.id === "add-project") {
    let n = draft.projects.length + 1;
    while (draft.projects.some((p) => p.slug === `yeniproje${n}`)) n++;
    const slug = `yeniproje${n}`;
    draft.projects.push({ id: slug, slug, name: "Yeni proje", tagline: "", kind: "app", status: "idea", icon: "", color: "#4a5462", platforms: [], tech: [], summary: "", features: [], links: [] });
    view = { name: "project", index: draft.projects.length - 1 };
  } else if (dataset.grant) {
    const a = attempts.find((x) => x.uid === dataset.grant);
    if (!confirm(`${a.email} hesabına yönetim erişimi verilsin mi?`)) return;
    await saveAdmins([...admins.emails, a.email.toLowerCase()]);
    return;
  } else if (dataset.forget) {
    await deleteDoc(doc(db, "loginAttempts", dataset.forget));
    attempts = attempts.filter((x) => x.uid !== dataset.forget);
    render();
    return;
  } else if (dataset.removeAdmin) {
    const email = admins.emails[Number(dataset.removeAdmin)];
    if (!confirm(`${email} erişimi kaldırılsın mı?`)) return;
    await saveAdmins(admins.emails.filter((e) => e !== email));
    return;
  } else {
    return;
  }
  render();
});

/* ---------- Kaydetme ---------- */

function cleanAndValidate() {
  const errors = [];
  const seen = new Set();
  for (const p of draft.projects) {
    const name = p.name?.trim() || "Adsız proje";
    if (!p.name?.trim()) errors.push("Bir projenin adı boş.");
    if (!/^[a-z0-9]+$/.test(p.slug || "")) errors.push(`${name}: adres yalnızca küçük harf ve rakam olabilir.`);
    else if (RESERVED_SLUGS.has(p.slug)) errors.push(`${name}: “${p.slug}” adresi sitede kullanılıyor.`);
    else if (seen.has(p.slug)) errors.push(`${name}: “${p.slug}” adresi başka projede de var.`);
    seen.add(p.slug);
    for (const key of ["features", "planned"]) if (p[key]) p[key] = p[key].map((s) => s.trim()).filter(Boolean);
    for (const r of p.changelog || []) r.items = (r.items || []).map((s) => s.trim()).filter(Boolean);
    for (const key of ["planned", "resources", "changelog"]) if (Array.isArray(p[key]) && !p[key].length) delete p[key];
    p.links = (p.links || []).filter((l) => l.label.trim() && l.url.trim() && l.url !== "https://");
  }
  return errors;
}

async function save() {
  const errors = cleanAndValidate();
  if (errors.length) {
    alert(`Kaydedilmedi:\n\n${errors.join("\n")}`);
    return;
  }
  const button = document.getElementById("save");
  button.disabled = true;
  document.getElementById("save-status").textContent = "Kaydediliyor…";
  try {
    await setDoc(doc(db, "site", "content"), { json: JSON.stringify(draft), updatedAt: serverTimestamp() });
    saved = clone(draft);
    toast("Kaydedildi, sitede yayında");
  } catch (error) {
    if (error.code === "permission-denied") {
      view = { name: "setup" };
      alert("Kaydetme izni yok. Kurulum sayfasındaki kuralları Firebase konsoluna yapıştırman gerekiyor.");
    } else {
      alert(`Kaydedilemedi: ${error.message}`);
    }
  } finally {
    button.disabled = false;
    document.getElementById("save-status").textContent = "Kaydedilmemiş değişiklikler var";
    render();
  }
}

document.getElementById("save").addEventListener("click", save);
document.getElementById("discard").addEventListener("click", () => {
  if (!confirm("Kaydedilmemiş değişiklikler silinsin mi?")) return;
  draft = clone(saved);
  render();
});
window.addEventListener("beforeunload", (event) => {
  if (draft && isDirty()) event.preventDefault();
});

/* ---------- Giriş ---------- */

function loginView(message = "") {
  rail.innerHTML = "";
  side.hidden = true;
  main.innerHTML = `
    <section class="login">
      <h1>flozaxx<span class="brand-admin">admin</span></h1>
      <p class="muted">Site içeriğini düzenlemek için Google hesabınla giriş yap.</p>
      ${message ? `<p class="login-error">${esc(message)}</p>` : ""}
      <button type="button" class="btn btn-primary" id="login">Google ile giriş yap</button>
    </section>`;
  document.getElementById("login").addEventListener("click", async () => {
    try {
      await signInWithPopup(auth, new GoogleAuthProvider());
    } catch (error) {
      loginView(error.code === "auth/unauthorized-domain"
        ? "Bu alan adı Firebase'de yetkili değil: Authentication → Ayarlar → Yetkili alan adları listesine ekle."
        : `Giriş yapılamadı: ${error.message}`);
    }
  });
}

// Erişim listesini yalnızca yetkili hesaplar okuyabilir (Firestore kuralları); okunamıyorsa bu hesap yetkisizdir.
async function checkAccess(user) {
  try {
    const snap = await getDoc(doc(db, ...ADMINS_PATH));
    if (snap.exists()) {
      admins = snap.data();
    } else {
      admins = { owner: user.uid, emails: [user.email.toLowerCase()] };
      await setDoc(doc(db, ...ADMINS_PATH), admins);
    }
    return true;
  } catch (error) {
    if (error.code !== "permission-denied") throw error;
    noAccessView(user);
    recordFailedLogin(user);
    return false;
  }
}

// Yetkisiz giriş kaydedilir; yöneticiler Erişim sayfasında görür. Başarılı girişler kaydedilmez.
async function recordFailedLogin(user) {
  try {
    await setDoc(doc(db, "loginAttempts", user.uid), {
      uid: user.uid,
      email: user.email,
      name: user.displayName || "",
      photo: user.photoURL || "",
      agent: navigator.userAgent.slice(0, 300),
      lang: navigator.language || "",
      count: increment(1),
      at: serverTimestamp(),
      seen: false,
    }, { merge: true });
  } catch {
    // Kayıt yazılamasa da (ör. kurallar eski) kullanıcıya gösterilecek bir şey yok.
  }
}

function noAccessView(user) {
  draft = null;
  rail.innerHTML = "";
  side.hidden = true;
  savebar.hidden = true;
  main.innerHTML = `
    <section class="login">
      <h1>flozaxx<span class="brand-admin">admin</span></h1>
      <p class="login-error">${esc(user.email)} hesabının yönetim paneline erişimi yok.</p>
      <button type="button" class="btn" id="switch-account">Başka hesapla giriş yap</button>
    </section>`;
  document.getElementById("switch-account").addEventListener("click", () => signOut(auth));
}

async function loadDraft() {
  const snap = await getDoc(doc(db, "site", "content"));
  saved = snap.exists() ? JSON.parse(snap.data().json) : { site: window.SITE, projects: window.PROJECTS };
  if (!snap.exists()) toast("İlk kurulum: mevcut içerik yüklendi, kaydedince veritabanına geçer");
  draft = clone(saved);
}

async function start() {
  const config = await (await fetch("/__/firebase/init.json")).json();
  const app = initializeApp(config);
  auth = getAuth(app);
  db = getFirestore(app);
  onAuthStateChanged(auth, async (user) => {
    const userEl = document.getElementById("admin-user");
    if (!user) {
      userEl.innerHTML = "";
      draft = null;
      savebar.hidden = true;
      loginView();
      return;
    }
    userEl.innerHTML = `<span class="hide-sm">${esc(user.displayName || user.email)}</span><button type="button" class="text-btn" id="logout">Çıkış</button>`;
    document.getElementById("logout").addEventListener("click", () => signOut(auth));
    main.innerHTML = '<p class="muted">Erişim kontrol ediliyor…</p>';
    if (!(await checkAccess(user))) return;
    await loadAttempts().catch(() => {
      // Kurallar henüz loginAttempts'i kapsamıyorsa liste boş kalır.
    });
    try {
      await loadDraft();
    } catch {
      saved = { site: window.SITE, projects: window.PROJECTS };
      draft = clone(saved);
      toast("Veritabanına ulaşılamadı; mevcut içerik yüklendi");
    }
    render();
  });
}

start().catch((error) => {
  main.innerHTML = `<p class="login-error">Panel başlatılamadı: ${esc(error.message)}</p>`;
});
