"use strict";

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

setupTocSpy();
