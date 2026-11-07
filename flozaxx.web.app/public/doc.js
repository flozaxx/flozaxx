"use strict";

const TASKS_KEY = "once-bak-sonra-yap:tasks";

function loadTasks() {
  try {
    return JSON.parse(localStorage.getItem(TASKS_KEY)) || {};
  } catch {
    return {};
  }
}

function saveTasks(tasks) {
  try {
    localStorage.setItem(TASKS_KEY, JSON.stringify(tasks));
  } catch {
    // Gizli sekmede kaydedilmez; işaretler sayfa açık kaldıkça durur.
  }
}

function setupChecklist() {
  const tasks = loadTasks();
  const boxes = [...document.querySelectorAll("[data-task]")];
  for (const box of boxes) {
    box.checked = Boolean(tasks[box.dataset.task]);
    box.addEventListener("change", () => {
      tasks[box.dataset.task] = box.checked;
      saveTasks(tasks);
    });
  }

  const lastList = boxes.at(-1)?.closest("ul");
  if (!lastList) return;
  const reset = document.createElement("button");
  reset.type = "button";
  reset.className = "text-btn checklist-reset";
  reset.textContent = "İşaretleri temizle";
  reset.addEventListener("click", () => {
    for (const box of boxes) box.checked = false;
    saveTasks({});
  });
  lastList.after(reset);
}

function setupTocSpy() {
  const links = [...document.querySelectorAll(".doc-toc [data-spy]")];
  const headings = links.map((a) => document.getElementById(a.dataset.spy));
  let frame = 0;
  const update = () => {
    frame = 0;
    let active = 0;
    headings.forEach((h, i) => {
      if (h.getBoundingClientRect().top < 120) active = i;
    });
    links.forEach((a, i) => a.classList.toggle("is-active", i === active));
  };
  window.addEventListener("scroll", () => {
    if (!frame) frame = requestAnimationFrame(update);
  }, { passive: true });
  update();
}

setupChecklist();
setupTocSpy();
