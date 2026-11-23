// Project site: four small things, none of them needed to read the page.
//   1. Language: English is in the HTML, German and Portuguese in data-de and
//      data-pt. The same mechanism and the same storage key as the S202 site.
//   2. The download buttons point directly at the files of the latest release.
//   3. A large view of the pictures.
//   4. Header line and fade-in while scrolling.
// Without JavaScript there remains: the English page, buttons that lead to the
// release page, and pictures that open as a file.
"use strict";

const LANGS = ["en", "de", "pt"];
const REPO = "Weigend/Software-EKG";
let lang = "en";
let release = null;   // the latest release, once GitHub has answered

// ---------- 1. Language ----------
// Attributes that can be translated: data-href-de, data-alt-pt, ...
const ATTRS = ["href", "alt", "content"];

function setLang(next, remember) {
  lang = LANGS.includes(next) ? next : "en";
  document.documentElement.lang = lang;
  document.querySelectorAll(".lang-btn").forEach((b) =>
    b.setAttribute("aria-pressed", String(b.dataset.lang === lang)));

  document.querySelectorAll("[data-de]").forEach((el) => {
    if (el.dataset.en === undefined) el.dataset.en = el.innerHTML;
    el.innerHTML = el.dataset[lang] ?? el.dataset.en;
  });
  for (const attr of ATTRS) {
    document.querySelectorAll(`[data-${attr}-de], [data-${attr}-pt]`).forEach((el) => {
      const en = `data-${attr}-en`;
      if (!el.hasAttribute(en)) el.setAttribute(en, el.getAttribute(attr));
      el.setAttribute(attr, el.getAttribute(`data-${attr}-${lang}`) ?? el.getAttribute(en));
    });
  }
  showRelease();
  if (remember) {
    try { localStorage.setItem("weigend-lang", lang); } catch { /* private mode */ }
  }
}

document.querySelectorAll(".lang-btn").forEach((b) =>
  b.addEventListener("click", () => setLang(b.dataset.lang, true)));

(function initialLanguage() {
  // Order: ?lang= in the address, the remembered choice, the language of the browser.
  const fromAddress = new URLSearchParams(location.search).get("lang");
  let remembered = null;
  try { remembered = localStorage.getItem("weigend-lang"); } catch { /* private mode */ }
  const browser = (navigator.language || "en").slice(0, 2);
  setLang([fromAddress, remembered, browser].find((l) => LANGS.includes(l)) || "en", false);
})();

// ---------- 2. Downloads ----------
// The buttons carry data-asset="workbench-linux-x64". That matches the file
// software-ekg-workbench-<version>-linux-x64.zip of the release.
function showRelease() {
  const line = document.getElementById("relLine");
  if (!release || !line) return;
  const version = (release.tag_name || "").replace(/^v/, "");
  const date = new Intl.DateTimeFormat(lang, { dateStyle: "long" }).format(new Date(release.published_at));
  const text = {
    en: `Version ${version} of ${date}, straight from GitHub.`,
    de: `Version ${version} vom ${date}, direkt von GitHub.`,
    pt: `Versão ${version} de ${date}, direto do GitHub.`,
  };
  line.textContent = text[lang];
}

fetch(`https://api.github.com/repos/${REPO}/releases/latest`, { headers: { Accept: "application/vnd.github+json" } })
  .then((r) => (r.ok ? r.json() : Promise.reject(r.status)))
  .then((rel) => {
    release = rel;
    document.querySelectorAll("a[data-asset]").forEach((a) => {
      const [kind, ...platform] = a.dataset.asset.split("-");
      const end = `-${platform.join("-")}.zip`;
      const asset = (rel.assets || []).find((x) =>
        x.name.startsWith(`software-ekg-${kind}-`) && x.name.endsWith(end));
      if (asset) a.href = asset.browser_download_url;
    });
    showRelease();
  })
  .catch(() => { /* the buttons then lead to the release page */ });

// Highlight the card of the visitor's operating system.
(function ownSystem() {
  const p = ((navigator.userAgentData && navigator.userAgentData.platform) || navigator.platform || "").toLowerCase();
  const os = p.includes("win") ? "windows" : p.includes("mac") ? "macos" : p.includes("linux") ? "linux" : null;
  if (/android|iphone|ipad/i.test(navigator.userAgent) || !os) return;
  const card = document.querySelector(`.card[data-os="${os}"]`);
  if (card) card.classList.add("yours");
})();

// ---------- 3. Large view ----------
(function lightbox() {
  const dialog = document.getElementById("lightbox");
  if (!dialog || typeof dialog.showModal !== "function") return;
  const image = document.getElementById("lightboxImg");
  const caption = document.getElementById("lightboxCap");
  document.querySelectorAll("a[data-lightbox]").forEach((a) => {
    a.addEventListener("click", (e) => {
      if (e.ctrlKey || e.metaKey || e.shiftKey) return;   // a new tab stays possible
      e.preventDefault();
      const small = a.querySelector("img");
      image.src = a.href;
      image.alt = small ? small.alt : "";
      caption.textContent = small ? small.alt : "";
      dialog.showModal();
    });
  });
  dialog.addEventListener("click", (e) => { if (e.target !== image) dialog.close(); });
})();

// ---------- 4. Header line and fade-in ----------
const nav = document.getElementById("navbar");
const onScroll = () => nav.classList.toggle("scrolled", window.scrollY > 20);
window.addEventListener("scroll", onScroll, { passive: true });
onScroll();

if ("IntersectionObserver" in window) {
  const observer = new IntersectionObserver((entries) => {
    for (const e of entries) {
      if (e.isIntersecting) { e.target.classList.add("visible"); observer.unobserve(e.target); }
    }
  }, { threshold: 0.08, rootMargin: "0px 0px -30px 0px" });
  document.querySelectorAll(".reveal").forEach((el) => observer.observe(el));
} else {
  document.querySelectorAll(".reveal").forEach((el) => el.classList.add("visible"));
}
