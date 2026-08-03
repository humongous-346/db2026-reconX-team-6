// File: static-dashboard/js/trades.js
(function () {
    const table = document.getElementById('trades-table');
    const tbody = document.getElementById('trades-tbody');
    let rows = []; // canonical data array
  
    if (!table || !tbody) return; // Safety check
  
    // --------- sortable columns
    table.querySelectorAll('thead th').forEach(th => {
      th.addEventListener('click', (e) => {
        // Skip sorting if the user is clicking the resize handle
        if (e.target.classList.contains('resize-handle')) return;
        
        const col = th.dataset.col;
        const type = th.dataset.type;
        const currentDir = th.getAttribute('aria-sort');
        const dir = currentDir === 'ascending' ? 'descending' : 'ascending';
  
        // Clear the sort indicator from all columns, then apply to this one
        table.querySelectorAll('thead th').forEach(h => h.setAttribute('aria-sort', 'none'));
        th.setAttribute('aria-sort', dir);
  
        const mult = dir === 'ascending' ? 1 : -1;
        
        rows.sort((a, b) => {
          const av = a[col];
          const bv = b[col];
          
          // Sort mathematically for numbers, alphabetically for strings
          if (type === 'number') {
             return (av - bv) * mult;
          }
          return String(av).localeCompare(String(bv)) * mult;
        });
        
        renderRows();
      });
    });
  
    // --------- resizable columns
    table.querySelectorAll('.resize-handle').forEach(handle => {
      handle.addEventListener('mousedown', (e) => {
        e.preventDefault();
        const th = handle.closest('th');
        const startX = e.clientX;
        const startWidth = th.offsetWidth;
  
        // Listen on DOCUMENT so the drag survives leaving the handle
        function onMove(ev) {
          th.style.width = (startWidth + ev.clientX - startX) + 'px';
        }
        
        function onUp() {
          document.removeEventListener('mousemove', onMove);
          document.removeEventListener('mouseup', onUp);
        }
        
        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
      });
    });
  
    // --------- Render function
    function renderRows() {
      tbody.innerHTML = rows.map(r => `
        <tr>
          <td>${r.tradeRef}</td>
          <td>${r.symbol}</td>
          <td>${r.quantity}</td>
          <td>${r.price}</td>
          <td>${r.status}</td>
        </tr>`).join('');
    }
  
    // --------- Initial load (hits the REST API)
    fetch('/api/v1/trades?size=200')
      .then(r => r.json())
      .then(data => {
        // Fallback for different pagination/response formats
        rows = data.content || data; 
        renderRows();
      })
      .catch(err => console.error("Failed to fetch initial trades:", err));
  })();