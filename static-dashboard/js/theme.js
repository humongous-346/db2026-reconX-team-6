// File: static-dashboard/js/theme.js
(function () {
  // Wait for the DOM to fully load before attaching the event listener
  document.addEventListener('DOMContentLoaded', () => {
    const btn = document.getElementById('theme-toggle');
    
    // Set initial aria-pressed state based on current theme
    if (btn) {
      const currentTheme = document.documentElement.dataset.theme;
      btn.setAttribute('aria-pressed', currentTheme === 'dark');
      
      btn.addEventListener('click', () => {
        // Calculate the next theme
        const next = document.documentElement.dataset.theme === 'light' ? 'dark' : 'light';
        
        // Apply it to the HTML tag
        document.documentElement.dataset.theme = next;
        
        // Save it to localStorage so it persists on reload
        localStorage.setItem('reconx-theme', next);
        
        // Update accessibility attribute
        btn.setAttribute('aria-pressed', next === 'dark');
      });
    }
  });
})();