// flozaxx yönetim paneli: siteyi besleyen site/content belgesini formlarla düzenler.
// Yalnızca giriş yapan sahibin yazabilmesini Firestore kuralları sağlar; bu sayfa yalnızca arayüzdür.
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import { getAuth, GoogleAuthProvider, signInWithPopup, onAuthStateChanged, signOut } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";
import { getFirestore, doc, getDoc, setDoc, serverTimestamp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";

const RESERVED_SLUGS = new Set(["admin", "download", "downloads", "p", "favicon", "page", "index", "404"]);
const KIND_OPTIONS = { app: "Uygulama", tool: "Rehber / araç", web: "Web" };
const STATUS_OPTIONS = { live: "Yayında", dev: "Geliştiriliyor", idea: "Planlanıyor" };

const root = document.getElementById("admin");
const savebar = document.getElementById("savebar");
let auth;
let db;
let saved = null;
let draft = null;
let tab = "projects";
let selected = 0;

const clone = (value) => JSON.parse(JSON.stringify(value));
const dirty = () => JSON.stringify(saved) !== JSON.stringify(draft);

function toast(message) {
  const el = document.getElementById("toast");
  el.textContent = message;
  el.classList.add("is-visible");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => el.classList.remove("is-visible"), 2200);
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

function syncSavebar() {
  savebar.hidden = !dirty();
}

/* ---------- Alan bileşenleri ---------- */

function field(label, path, { type = "text", hint = "", placeholder = "" } = {}) {
  const value = getPath(path);
  const id = `f-${path.replace(/\./g, "-")}`;
  let input;
  if (type === "textarea" || type === "lines") {
    const text = type === "lines" ? (value || []).join("\n") : value || "";
    input = `<textarea id="${id}" data-path="${path}" data-type="${type}" rows="${type === "lines" ? Math.max(3, (value || []).length + 1) : 4}" placeholder="${esc(placeholder)}">${esc(text)}</textarea>`;
  } else if (type === "csv") {
    input = `<input id="${id}" data-path="${path}" data-type="csv" value="${esc((value || []).join(", "))}" placeholder="${esc(placeholder)}">`;
  } else {
    input = `<input id="${id}" data-path="${path}" data-type="text" value="${esc(value ?? "")}" placeholder="${esc(placeholder)}">`;
  }
  return `<label class="field" for="${id}"><span class="field-label">${label}</span>${input}${hint ? `<span class="field-hint">${hint}</span>` : ""}</label>`;
}

function selectField(label, path, options) {
  const value = getPath(path);
  const opts = Object.entries(options).map(([k, v]) => `<option value="${esc(k)}"${k === value ? " selected" : ""}>${esc(v)}</option>`).join("");
  return `<label class="field"><span class="field-label">${label}</span><select data-path="${path}" data-type="text">${opts}</select></label>`;
}

function colorField(label, path) {
  const value = getPath(path) || "#82b6f4";
  const solid = /^#[0-9a-f]{6}$/i.test(value) ? value : "#82b6f4";
  return `<label class="field"><span class="field-label">${label}</span>
    <span class="color-row"><input type="color" value="${solid}" data-path="${path}" data-type="text" aria-label="Renk seç"><input data-path="${path}" data-type="text" value="${esc(value)}" class="color-text"></span>
    <span class="field-hint">Düz renk ya da <code>linear-gradient(...)</code>.</span></label>`;
}

function rowControls(listPath, i, length) {
  return `<span class="row-controls">
    <button type="button" class="mini" data-move="${listPath}" data-index="${i}" data-dir="-1" ${i === 0 ? "disabled" : ""} aria-label="Yukarı taşı">↑</button>
    <button type="button" class="mini" data-move="${listPath}" data-index="${i}" data-dir="1" ${i === length - 1 ? "disabled" : ""} aria-label="Aşağı taşı">↓</button>
    <button type="button" class="mini mini-danger" data-remove="${listPath}" data-index="${i}" aria-label="Sil">✕</button>
  </span>`;
}

function listEditor(title, listPath, renderItem, addLabel, template) {
  const list = getPath(listPath) || [];
  const rows = list.map((item, i) => `<div class="row-card"><div class="row-head"><span class="row-no">${i + 1}</span>${rowControls(listPath, i, list.length)}</div>${renderItem(`${listPath}.${i}`, item, i)}</div>`).join("");
  return `<section class="edit-section"><h3>${title}</h3>${rows || '<p class="muted">Henüz yok.</p>'}
    <button type="button" class="btn btn-sm" data-add="${listPath}" data-template='${esc(JSON.stringify(template))}'>+ ${addLabel}</button></section>`;
}

function projectOptions() {
  return Object.fromEntries(draft.projects.map((p) => [p.id, p.name || p.slug]));
}

/* ---------- Görünümler ---------- */

function previewHtml(p) {
  const card = `
    <div class="project preview-card">
      ${tileHtml(p)}
      <span class="project-text">
        <span class="project-title">${esc(p.name || "Adsız proje")}${statusHtml(p)}</span>
        <span class="project-tagline">${esc(p.tagline || "")}</span>
        <span class="project-meta">${esc((p.platforms || []).join(" · "))}</span>
      </span>
      <span class="go">Projeye git${UI_ICONS.arrow}</span>
    </div>`;
  return `<div class="preview">${card}<a class="text-link" href="https://flozaxx.com/${esc(p.slug)}" target="_blank" rel="noopener">Sitedeki sayfasını aç${UI_ICONS.external}</a></div>`;
}

function projectEditor(i) {
  const p = draft.projects[i];
  const base = `projects.${i}`;
  const iconOptions = Object.fromEntries([["", "Baş harf"], ...Object.keys(ICONS).map((k) => [k, k])]);
  const hasDownload = Boolean(p.download);
  return `
    <div id="preview-slot">${previewHtml(p)}</div>
    <section class="edit-section">
      <h3>Temel bilgiler</h3>
      <div class="grid-2">
        ${field("Ad", `${base}.name`)}
        ${field("Adres (slug)", `${base}.slug`, { hint: `flozaxx.com/<b>${esc(p.slug)}</b> · yalnızca küçük harf ve rakam` })}
      </div>
      ${field("Kısa açıklama", `${base}.tagline`)}
      <div class="grid-3">
        ${selectField("Tür", `${base}.kind`, KIND_OPTIONS)}
        ${selectField("Durum", `${base}.status`, STATUS_OPTIONS)}
        ${selectField("İkon", `${base}.icon`, iconOptions)}
      </div>
      <div class="grid-2">
        ${colorField("İkon zemini", `${base}.color`)}
        ${field("İkon rengi", `${base}.ink`, { placeholder: "#fff" })}
      </div>
      <div class="grid-2">
        ${field("Platformlar", `${base}.platforms`, { type: "csv", placeholder: "Web, Android" })}
        ${field("Teknolojiler", `${base}.tech`, { type: "csv", placeholder: "Flutter, Firebase" })}
      </div>
      <div class="grid-3">
        ${field("Sürüm", `${base}.version`)}
        ${field("Başlangıç", `${base}.started`, { placeholder: "Ekim 2026" })}
        ${field("Güvenlik notu", `${base}.security`)}
      </div>
    </section>
    <section class="edit-section">
      <h3>Sayfa içeriği</h3>
      ${field("Açıklama", `${base}.summary`, { type: "textarea" })}
      ${field("Öne çıkanlar", `${base}.features`, { type: "lines", hint: "Her satır bir madde." })}
      ${field("Planlananlar", `${base}.planned`, { type: "lines", hint: "Her satır bir madde. Boş bırakılırsa bölüm görünmez." })}
    </section>
    ${listEditor("Bağlantı düğmeleri", `${base}.links`, (path) => `<div class="grid-2">${field("Yazı", `${path}.label`)}${field("Adres", `${path}.url`)}</div>`, "Bağlantı ekle", { label: "", url: "https://" })}
    <section class="edit-section">
      <h3>İndirme</h3>
      <label class="check"><input type="checkbox" data-download="${i}" ${hasDownload ? "checked" : ""}> Bu projenin indirilebilir dosyası var</label>
      ${hasDownload ? `${field("Platform", `${base}.download.platform`, { placeholder: "Android" })}
        <p class="field-hint">Dosyayı <a href="https://github.com/flozaxx/downloads/releases" target="_blank" rel="noopener">github.com/flozaxx/downloads</a> deposunda <b>${esc(p.slug)}</b> etiketli release'e <b>${esc(p.slug)}.apk</b> adıyla yükle. Kalıcı bağlantı: flozaxx.com/${esc(p.slug)}/dllatest</p>` : ""}
    </section>
    ${listEditor("Ek kaynaklar", `${base}.resources`, (path) => `<div class="grid-2">${field("Ad", `${path}.name`)}${field("Adres", `${path}.url`)}</div>${field("Açıklama", `${path}.text`)}`, "Kaynak ekle", { name: "", url: "https://", text: "" })}
    ${listEditor("Sürüm geçmişi (en yeni en üstte)", `${base}.changelog`, (path) => `<div class="grid-3">${field("Sürüm", `${path}.version`)}${field("Tarih", `${path}.date`)}${field("Başlık", `${path}.title`)}</div>${field("Değişiklikler", `${path}.items`, { type: "lines", hint: "Her satır bir madde." })}`, "Sürüm ekle", { version: "", date: "", title: "", items: [] })}
    <section class="edit-section danger-zone">
      <button type="button" class="btn btn-sm btn-danger" data-delete-project="${i}">Projeyi sil</button>
    </section>`;
}

function projectsTab() {
  const items = draft.projects.map((p, i) => `
    <li class="${i === selected ? "is-selected" : ""}">
      <button type="button" class="proj-pick" data-select="${i}">${tileHtml(p)}<span><b>${esc(p.name || "Adsız")}</b><small>${STATUS_OPTIONS[p.status] || ""}</small></span></button>
      <span class="row-controls">
        <button type="button" class="mini" data-move="projects" data-index="${i}" data-dir="-1" ${i === 0 ? "disabled" : ""} aria-label="Yukarı taşı">↑</button>
        <button type="button" class="mini" data-move="projects" data-index="${i}" data-dir="1" ${i === draft.projects.length - 1 ? "disabled" : ""} aria-label="Aşağı taşı">↓</button>
      </span>
    </li>`).join("");
  return `
    <div class="admin-cols">
      <aside class="proj-list">
        <ul>${items}</ul>
        <button type="button" class="btn btn-sm" id="add-project">+ Yeni proje</button>
        <p class="field-hint">Sitede yayında olanlar üstte, geliştirilenler altta gösterilir; aynı durumdakiler bu sırayı izler.</p>
      </aside>
      <div class="proj-editor">${draft.projects.length ? projectEditor(selected) : '<p class="muted">Proje yok.</p>'}</div>
    </div>`;
}

function homeTab() {
  const options = projectOptions();
  const projectSelect = (path) => selectField("Proje", path, options);
  return `
    <section class="edit-section">
      <h3>Genel</h3>
      ${field("Son güncelleme tarihi (alt bilgide)", "site.updated", { placeholder: "8 Kasım 2026" })}
    </section>
    ${listEditor("Şu an (sağ sütun)", "site.now", (path) => `${projectSelect(`${path}.project`)}${field("Yazı", `${path}.text`)}`, "Satır ekle", { project: draft.projects[0]?.id || "", text: "" })}
    ${listEditor("Son güncellemeler (en yeni en üstte; sitede ilk 4'ü görünür)", "site.updates", (path) => `<div class="grid-2">${field("Tarih", `${path}.date`, { placeholder: "8 Kas 2026" })}${projectSelect(`${path}.project`)}</div>${field("Yazı", `${path}.text`)}`, "Güncelleme ekle", { date: "", project: draft.projects[0]?.id || "", text: "" })}`;
}

function setupTab(uid) {
  const rules = `rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /site/content {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == "${uid}";
    }
  }
}`;
  return `
    <section class="edit-section">
      <h3>Kurulum</h3>
      <p>Kaydetmenin çalışması için Firestore kurallarının yalnızca senin hesabına yazma izni vermesi gerekir. Bir kez yapılır:</p>
      <ol class="setup-steps">
        <li><a href="https://console.firebase.google.com/project/flozaxx-a8521/firestore/rules" target="_blank" rel="noopener">Firebase konsolu → Firestore → Kurallar</a> sayfasını aç.</li>
        <li>Oradaki metnin tamamını silip aşağıdakini yapıştır, <b>Yayınla</b>'ya bas.</li>
      </ol>
      <div class="rules-box"><pre id="rules-text">${esc(rules)}</pre><button type="button" class="btn btn-sm" id="copy-rules">Kopyala</button></div>
      <p class="field-hint">Hesap kimliğin: <code>${esc(uid)}</code>. Kurallar herkese yalnızca okuma izni verir; yazmayı yalnızca bu kimlik yapabilir.</p>
    </section>`;
}

function render() {
  if (!draft) return;
  const uid = auth.currentUser.uid;
  const tabs = [["projects", "Projeler"], ["home", "Ana sayfa"], ["setup", "Kurulum"]]
    .map(([key, label]) => `<button type="button" class="tab${tab === key ? " is-active" : ""}" data-tab="${key}">${label}</button>`).join("");
  root.innerHTML = `<nav class="tabs">${tabs}</nav>${tab === "projects" ? projectsTab() : tab === "home" ? homeTab() : setupTab(uid)}`;
  syncSavebar();
}

/* ---------- Düzenleme olayları ---------- */

function readInput(el) {
  const raw = el.value;
  if (el.dataset.type === "lines") return raw.split("\n").map((s) => s.trim()).filter(Boolean);
  if (el.dataset.type === "csv") return raw.split(",").map((s) => s.trim()).filter(Boolean);
  return raw;
}

function refreshPreview() {
  if (tab !== "projects") return;
  const p = draft.projects[selected];
  const slot = document.getElementById("preview-slot");
  if (slot && p) slot.innerHTML = previewHtml(p);
  const pick = root.querySelector(`[data-select="${selected}"]`);
  if (pick && p) pick.innerHTML = `${tileHtml(p)}<span><b>${esc(p.name || "Adsız")}</b><small>${STATUS_OPTIONS[p.status] || ""}</small></span>`;
}

root.addEventListener("input", (event) => {
  const el = event.target.closest("[data-path]");
  if (!el) return;
  const value = readInput(el);
  const optional = ["ink", "version", "started", "security"];
  const key = el.dataset.path.split(".").pop();
  setPath(el.dataset.path, value === "" && optional.includes(key) ? undefined : value);
  if (el.type === "color") root.querySelector(`.color-text[data-path="${el.dataset.path}"]`).value = value;
  if (key === "slug" && el.dataset.path.startsWith("projects.") && el.dataset.path.split(".").length === 3) {
    const p = getPath(el.dataset.path.split(".").slice(0, 2).join("."));
    if (p.download) p.download.file = `${value}.apk`;
  }
  refreshPreview();
  syncSavebar();
});

root.addEventListener("change", (event) => {
  const el = event.target;
  if (el.matches("[data-download]")) {
    const p = draft.projects[Number(el.dataset.download)];
    if (el.checked) p.download = { file: `${p.slug}.apk`, platform: "Android" };
    else delete p.download;
    render();
  } else if (el.matches("select[data-path]")) {
    refreshPreview();
  }
});

root.addEventListener("click", async (event) => {
  const btn = event.target.closest("button");
  if (!btn) return;
  const { dataset } = btn;
  if (dataset.tab) {
    tab = dataset.tab;
  } else if (dataset.select) {
    selected = Number(dataset.select);
    render();
    // Telefonda düzenleyici listenin altında kalır; seçilen projeye kaydırılır.
    if (!window.matchMedia("(min-width: 900px)").matches) document.querySelector(".proj-editor").scrollIntoView({ behavior: "smooth" });
    return;
  } else if (dataset.move) {
    const list = getPath(dataset.move);
    const i = Number(dataset.index);
    const j = i + Number(dataset.dir);
    [list[i], list[j]] = [list[j], list[i]];
    if (dataset.move === "projects" && (selected === i || selected === j)) selected = selected === i ? j : i;
  } else if (dataset.remove) {
    getPath(dataset.remove).splice(Number(dataset.index), 1);
  } else if (dataset.add) {
    if (!getPath(dataset.add)) setPath(dataset.add, []);
    getPath(dataset.add).push(JSON.parse(dataset.template));
  } else if (dataset.deleteProject) {
    const p = draft.projects[Number(dataset.deleteProject)];
    if (!confirm(`“${p.name}” silinsin mi? Kaydedene kadar geri alabilirsin.`)) return;
    draft.projects.splice(Number(dataset.deleteProject), 1);
    selected = Math.max(0, selected - 1);
  } else if (btn.id === "add-project") {
    const slug = `yeniproje${draft.projects.length + 1}`;
    draft.projects.push({
      id: slug, slug, name: "Yeni proje", tagline: "", kind: "app", status: "idea", icon: "", color: "#4a5462",
      platforms: [], tech: [], summary: "", features: [], links: [],
    });
    selected = draft.projects.length - 1;
  } else if (btn.id === "copy-rules") {
    await navigator.clipboard.writeText(document.getElementById("rules-text").textContent);
    toast("Kurallar kopyalandı");
    return;
  } else {
    return;
  }
  render();
});

/* ---------- Kaydetme ---------- */

function validate() {
  const errors = [];
  const seen = new Set();
  draft.projects.forEach((p) => {
    const name = p.name || "Adsız proje";
    if (!p.name?.trim()) errors.push("Bir projenin adı boş.");
    if (!/^[a-z0-9]+$/.test(p.slug || "")) errors.push(`${name}: adres yalnızca küçük harf ve rakam olabilir.`);
    if (RESERVED_SLUGS.has(p.slug)) errors.push(`${name}: “${p.slug}” adresi sitede kullanılıyor, başka bir ad seç.`);
    if (seen.has(p.slug)) errors.push(`${name}: “${p.slug}” adresi başka bir projede de var.`);
    seen.add(p.slug);
    for (const key of ["planned", "resources", "changelog", "links"]) {
      if (Array.isArray(p[key]) && !p[key].length && key !== "links") delete p[key];
    }
  });
  return errors;
}

async function save() {
  const errors = validate();
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
      tab = "setup";
      alert("Kaydetme izni yok. Kurulum sekmesindeki kuralları Firebase konsoluna yapıştırman gerekiyor.");
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
  selected = Math.min(selected, draft.projects.length - 1);
  render();
});
window.addEventListener("beforeunload", (event) => {
  if (draft && dirty()) event.preventDefault();
});

/* ---------- Giriş ---------- */

function loginView(message = "") {
  root.innerHTML = `
    <section class="login">
      <h1>Yönetim</h1>
      <p class="muted">flozaxx.com içeriğini düzenlemek için Google hesabınla giriş yap.</p>
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

async function loadDraft() {
  const snap = await getDoc(doc(db, "site", "content"));
  if (snap.exists()) {
    saved = JSON.parse(snap.data().json);
  } else {
    // İlk açılış: veritabanında içerik yoksa sitedeki mevcut içerikten başlanır.
    saved = { site: window.SITE, projects: window.PROJECTS };
    toast("İlk kurulum: mevcut içerik yüklendi, kaydedince veritabanına geçer");
  }
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
    userEl.innerHTML = `<span>${esc(user.displayName || user.email)}</span><button type="button" class="text-btn" id="logout">Çıkış</button>`;
    document.getElementById("logout").addEventListener("click", () => signOut(auth));
    root.innerHTML = '<p class="admin-loading">İçerik yükleniyor…</p>';
    try {
      await loadDraft();
    } catch (error) {
      saved = { site: window.SITE, projects: window.PROJECTS };
      draft = clone(saved);
      tab = "setup";
      toast(error.code === "permission-denied" ? "Okuma izni yok; önce Kurulum adımını yap" : "Veritabanına ulaşılamadı; mevcut içerik yüklendi");
    }
    render();
  });
}

start().catch((error) => {
  root.innerHTML = `<p class="login-error">Panel başlatılamadı: ${esc(error.message)}</p>`;
});
