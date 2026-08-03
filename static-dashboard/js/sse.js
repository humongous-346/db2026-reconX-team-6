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

  // TICKET-ADV105: Step 5 - XSS guard helper
  function escapeHtml(s) {
    if (!s) return '';
    return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  // TICKET-ADV105: Step 6 - Number formatters
  const formatQty = new Intl.NumberFormat('en-US').format;
  const formatPrice = new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4
  }).format;

  // TICKET-ADV105: Step 1 - Refactored prepend function
  function prependTradeRow(trade) {
    // Step 2: Map trade.status to a CSS modifier
    let statusModifier = '';
    if (trade.status === 'MATCHED') {
      statusModifier = 'trade-card--matched';
    } else if (trade.status === 'UNMATCHED') {
      statusModifier = 'trade-card--break';
    }

    // Step 3: Create element and assign classes
    const row = document.createElement('article');
    row.className = 'trade-card ' + statusModifier + ' trade-card--new';

    // Step 4: Set innerHTML with safe interpolation and formatters
    row.innerHTML = `
      <header class="trade-card__header">
        <strong>${escapeHtml(trade.tradeRef)}</strong>
        <span>[${escapeHtml(trade.status)}]</span>
      </header>
      <div class="trade-card__body">
        <span>${escapeHtml(trade.symbol)}</span>
        <span>qty=${formatQty(trade.qty)}</span>
        <span>price=${formatPrice(trade.price)} ${escapeHtml(trade.currency || '')}</span>
      </div>
    `;

    // Step 7: Prepend to DOM and remove animation class after 500ms
    feed.prepend(row);
    setTimeout(() => {
      row.classList.remove('trade-card--new');
    }, 500);

    // Step 8: Cap the DOM to a maximum of 50 entries
    while (feed.children.length > 50) {
      feed.lastElementChild.remove();
    }
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
    };
  }

  // Clean up on navigation
  window.addEventListener('beforeunload', () => {
    sse?.close();
  });

  // Initialize
  connect();
})();