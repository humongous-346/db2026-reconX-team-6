import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TradeRow } from '@components/TradeRow.jsx';

describe('TradeRow', () => {
  it('renders the trade fields and calls the handler when clicked', async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();

    render(
      <TradeRow
        trade={{ id: 7, tradeRef: 'AAA-20240101-0007', instrument: 'AAPL', quantity: 100, price: 123.45, status: 'MATCHED' }}
        onClick={onClick}
      />
    );

    expect(screen.getByText('AAA-20240101-0007')).toBeInTheDocument();
    expect(screen.getByText('AAPL')).toBeInTheDocument();
    expect(screen.getByText('MATCHED')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /AAA-20240101-0007/i }));
    expect(onClick).toHaveBeenCalledWith(7);
  });
});
