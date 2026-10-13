(() => {
  const navigation = document.querySelector('.nav');
  const button = navigation?.querySelector('.nav-toggle');
  const links = navigation?.querySelector('.navlinks');
  if (!button || !links) return;

  const setOpen = (open) => {
    links.dataset.open = String(open);
    button.setAttribute('aria-expanded', String(open));
    button.textContent = open ? 'Close menu' : 'Menu';
  };
  button.hidden = false;
  navigation.classList.add('nav-ready');
  setOpen(false);
  button.addEventListener('click', () => setOpen(button.getAttribute('aria-expanded') !== 'true'));
  links.addEventListener('click', (event) => {
    if (event.target.closest('a')) setOpen(false);
  });
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && button.getAttribute('aria-expanded') === 'true') {
      setOpen(false);
      button.focus();
    }
  });
  document.addEventListener('click', (event) => {
    if (!navigation.contains(event.target)) setOpen(false);
  });
})();
