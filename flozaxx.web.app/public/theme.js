"use strict";

(() => {
  const btn = document.getElementById("theme-toggle");
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
})();
