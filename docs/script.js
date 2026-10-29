// Kush Acres site: drifting smoke, falling leaves, a lightbox and the latest release from GitHub.
(() => {
  const still = matchMedia("(prefers-reduced-motion: reduce)").matches;

  // Smoke: soft puffs rising from the bottom of the page.
  const cv = document.getElementById("smoke");
  const cx = cv.getContext("2d");
  let W = 0, H = 0;
  const puffs = [];
  const size = () => { W = cv.width = innerWidth; H = cv.height = innerHeight; };
  size(); addEventListener("resize", size);
  const puff = (y) => ({
    x: Math.random() * W, y: y ?? H + 60, r: 40 + Math.random() * 90,
    vx: (Math.random() - 0.5) * 0.25, vy: -0.25 - Math.random() * 0.45,
    a: 0, max: 0.05 + Math.random() * 0.07, hue: Math.random() < 0.75 ? 110 : 45,
  });
  for (let i = 0; i < 26; i++) puffs.push(puff(Math.random() * H));
  const tick = () => {
    cx.clearRect(0, 0, W, H);
    for (const p of puffs) {
      p.x += p.vx + Math.sin(p.y / 90) * 0.25; p.y += p.vy; p.r += 0.08;
      p.a = Math.min(p.max, p.a + 0.0008);
      const fade = Math.min(1, p.y / (H * 0.35));
      const g = cx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.r);
      g.addColorStop(0, `hsla(${p.hue}, 30%, 75%, ${p.a * fade})`);
      g.addColorStop(1, `hsla(${p.hue}, 30%, 75%, 0)`);
      cx.fillStyle = g; cx.beginPath(); cx.arc(p.x, p.y, p.r, 0, Math.PI * 2); cx.fill();
      if (p.y + p.r < 0) Object.assign(p, puff());
    }
    requestAnimationFrame(tick);
  };
  if (!still) tick();

  // Leaves: a few pixel fan leaves drifting down.
  const box = document.getElementById("leaves");
  const leaf = () => {
    if (document.hidden) return;
    const l = document.createElement("i");
    l.className = "fall";
    const s = 14 + Math.random() * 18, d = 9 + Math.random() * 9;
    Object.assign(l.style, { left: Math.random() * 100 + "vw", width: s + "px", height: s + "px", animationDuration: d + "s" });
    l.style.setProperty("--dx", (Math.random() - 0.5) * 300 + "px");
    l.style.setProperty("--rot", (Math.random() - 0.5) * 900 + "deg");
    box.appendChild(l);
    setTimeout(() => l.remove(), d * 1000);
  };
  if (!still) setInterval(leaf, 1400);

  // Lightbox for the gallery, maps and panels.
  const lb = document.getElementById("lightbox");
  document.querySelectorAll(".gallery a, .maps a, .panels a").forEach((a) => a.addEventListener("click", (e) => {
    e.preventDefault();
    lb.querySelector("img").src = a.href;
    lb.querySelector("p").textContent = a.querySelector("span")?.textContent ?? "";
    lb.hidden = false;
  }));
  const close = () => { lb.hidden = true; };
  lb.addEventListener("click", close);
  addEventListener("keydown", (e) => { if (e.key === "Escape") close(); });

  // The latest version and how many times it's been downloaded.
  fetch("https://api.github.com/repos/M4S4T0-V01D/kush-acres/releases?per_page=100")
    .then((r) => (r.ok ? r.json() : Promise.reject()))
    .then((rs) => {
      const latest = rs.find((r) => !r.draft && !r.prerelease);
      if (latest) document.getElementById("ver").textContent = latest.tag_name;
      const n = rs.flatMap((r) => r.assets).reduce((s, a) => s + a.download_count, 0);
      if (n > 0) document.getElementById("dl").textContent = n.toLocaleString() + " downloads";
    })
    .catch(() => {});
})();
