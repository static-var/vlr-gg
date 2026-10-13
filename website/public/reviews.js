document.querySelectorAll('[data-reviews]').forEach(section => {
  const track = section.querySelector('[data-reviews-track]');
  const cards = [...track.children];
  if (cards.length < 2) return;
  const previous = section.querySelector('[data-reviews-prev]');
  const next = section.querySelector('[data-reviews-next]');
  const toggle = section.querySelector('[data-reviews-toggle]');
  const controls = toggle.parentElement;
  const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
  let paused = false;
  let hovering = false;
  let focusing = false;
  let span = 0;
  let last = null;
  let drift = 0;
  let frame;

  const step = () => cards[0].getBoundingClientRect().width + parseFloat(getComputedStyle(track).gap);
  const normalize = () => {
    if (!span) return;
    if (track.scrollLeft < span) track.scrollLeft += span;
    else if (track.scrollLeft >= span * 2) track.scrollLeft -= span;
  };
  const updateButton = () => {
    toggle.textContent = paused ? 'Resume scrolling' : 'Pause scrolling';
    toggle.setAttribute('aria-pressed', String(paused));
  };
  const pause = () => { paused = true; updateButton(); };
  const configure = () => {
    track.querySelectorAll('[data-review-clone]').forEach(clone => clone.remove());
    span = 0;
    if (!reducedMotion.matches) {
      const copy = card => {
        const clone = card.cloneNode(true);
        clone.dataset.reviewClone = '';
        clone.setAttribute('aria-hidden', 'true');
        clone.inert = true;
        return clone;
      };
      track.prepend(...cards.map(copy));
      track.append(...cards.map(copy), ...cards.map(copy));
      span = cards[0].offsetLeft - track.firstElementChild.offsetLeft;
    }
    track.scrollLeft = span;
    toggle.hidden = reducedMotion.matches;
    last = null;
  };
  const animate = time => {
    if (last !== null && span && !paused && !hovering && !focusing && !document.hidden) {
      drift += Math.min(time - last, 50) * .022;
      const distance = Math.floor(drift);
      drift -= distance;
      track.scrollLeft += distance;
      normalize();
    }
    last = time;
    frame = requestAnimationFrame(animate);
  };
  const move = direction => {
    pause();
    track.scrollLeft += direction * step();
    normalize();
  };
  previous.addEventListener('click', () => move(-1));
  next.addEventListener('click', () => move(1));
  toggle.addEventListener('click', () => { paused = !paused; updateButton(); });
  section.addEventListener('mouseenter', () => { hovering = true; });
  section.addEventListener('mouseleave', () => { hovering = false; last = null; });
  section.addEventListener('focusin', () => { focusing = true; });
  section.addEventListener('focusout', event => { focusing = section.contains(event.relatedTarget); last = null; });
  track.addEventListener('pointerdown', pause);
  track.addEventListener('wheel', pause, {passive:true});
  track.addEventListener('scroll', normalize, {passive:true});
  reducedMotion.addEventListener('change', configure);
  new ResizeObserver(() => {
    if (reducedMotion.matches) return;
    span = cards[0].offsetLeft - track.firstElementChild.offsetLeft;
    normalize();
  }).observe(track);
  controls.hidden = false;
  configure();
  updateButton();
  frame = requestAnimationFrame(animate);
  window.addEventListener('pagehide', () => cancelAnimationFrame(frame));
  window.addEventListener('pageshow', event => {
    if (event.persisted) { last = null; frame = requestAnimationFrame(animate); }
  });
});
