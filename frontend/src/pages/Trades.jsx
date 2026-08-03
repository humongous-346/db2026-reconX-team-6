// TICKET-ADV114 — Compound DataTable.
// TICKET-ADV117 — useDebouncedSearch.
// TICKET-ADV119 / ADV121 — memoised rows and stable selection handler.
import React, { useCallback, useEffect, useState } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import DataTable from '@components/DataTable.jsx';
import { TradeRow } from '@components/TradeRow.jsx';
import { useDebouncedSearch } from '@hooks/useDebouncedSearch.js';
import { api } from '@services/apiService.js';

function Trades() {
  const [search, setSearch] = useState('');
  const debounced = useDebouncedSearch(search, 300);
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState(null);
  const [data, setData] = useState({ items: [], totalPages: 0 });

  const handleSelect = useCallback((id) => setSelectedId(id), []);

  useEffect(() => {
    let cancelled = false;
    const params = new URLSearchParams();
    params.set('page', String(page));
    if (debounced) params.set('status', debounced);

    api.listTrades(params.toString())
      .then((res) => {
        if (cancelled) return;
        if (res && Array.isArray(res.items)) {
          setData({ items: res.items, totalPages: res.totalPages ?? 0 });
        } else if (Array.isArray(res)) {
          setData({ items: res, totalPages: 1 });
        } else {
          setData({ items: [], totalPages: 0 });
        }
      })
      .catch(() => {
        if (!cancelled) setData({ items: [], totalPages: 0 });
      });

    return () => { cancelled = true; };
  }, [page, debounced]);

  return (
    <section>
      <h2>Trades</h2>
      <input
        aria-label="Filter by status"
        placeholder="status filter (PENDING/MATCHED/…)"
        value={search}
        onChange={(e) => setSearch(e.target.value.toUpperCase())}
      />
      <p aria-live="polite">Selected trade: {selectedId ?? 'none'}</p>
      <DataTable>
        <DataTable.Header columns={[
          { key: 'tradeRef', label: 'Ref' },
          { key: 'symbol',   label: 'Symbol' },
          { key: 'qty',      label: 'Qty' },
          { key: 'price',    label: 'Price' },
          { key: 'status',   label: 'Status' },
        ]} />
        <DataTable.Body
          rows={data.items}
          render={(t) => (
            <TradeRow trade={t} onClick={handleSelect} />
          )}
        />
        <DataTable.Pagination
          page={page}
          totalPages={Math.max(1, data.totalPages)}
          onChange={setPage}
        />
      </DataTable>
    </section>
  );
}

export default withAuth(Trades);
