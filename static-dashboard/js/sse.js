// File: static-dashboard/js/sse.js
(function () {
  const feed = document.getElementById('trade-feed');
  if (!feed) return;

  const badge = document.getElementById('sse-status');
  const STREAM_URL = '/api/v1/trades/stream';
  let sse = null;

  // Helper to update the connection badge UI
  function updateConnectionBadge(text, variant) {
    if (!badge) return;
    badge.textContent = text;
    
    if (variant === 'success') {
      badge.style.backgroundColor = 'var(--color-success)';
      badge.style.color = '#fff';
    } else {
      badge.style.backgroundColor = 'var(--color-warning)';
      badge.style.color = 'var(--color-text)';
    }
  }

  // Prepend new trade row
  function prependTradeRow(trade) {
    const el = document.createElement('div');
    // Using the combined entrance modifier from ADV102
    el.className = 'trade-card trade-card--new'; 
    el.innerHTML = `
      <strong>${trade.tradeRef || 'UNKNOWN'}</strong>
      <span>${trade.symbol || ''}</span>
      <span>qty=${trade.qty || 0}</span>
      <span>price=${trade.price || 0}</span>
    `;
    feed.prepend(el);
  }

  // Main connection function
  function connect() {
    sse = new EventSource(STREAM_URL);

    sse.onopen = () => {
      updateConnectionBadge('Live', 'success');
    };

    sse.onmessage = (event) => {
      try {
        const trade = JSON.parse(event.data);
        prependTradeRow(trade);
      } catch (err) {
        console.error('Failed to parse SSE message:', err);
      }
    };

    sse.onerror = () => {
      updateConnectionBadge('Reconnecting...', 'warning');
      // The browser automatically reconnects; do not call connect() here.
    };
  }

  // Clean up on navigation
  window.addEventListener('beforeunload', () => {
    sse?.close();
  });

  // Initialize
  connect();
})();