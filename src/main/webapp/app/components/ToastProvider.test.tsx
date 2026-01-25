'use client';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ToastProvider, { useToasts } from './ToastProvider';

function ToastHarness({ onAction }: { onAction: () => void }) {
  const { pushToast } = useToasts();
  return (
    <div>
      <button
        type="button"
        onClick={() =>
          pushToast('Saved', 'success', 'Note', {
            label: 'Undo',
            handler: onAction,
          })
        }
      >
        Push
      </button>
      <button type="button" onClick={() => pushToast('Hello', 'info')}>
        PushNoTitle
      </button>
    </div>
  );
}

describe('ToastProvider', () => {
  test('pushToast renders a toast and action', async () => {
    const user = userEvent.setup();
    const onAction = jest.fn();

    render(
      <ToastProvider>
        <ToastHarness onAction={onAction} />
      </ToastProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'Push' }));

    expect(screen.getByText('Saved')).toBeInTheDocument();
    expect(screen.getByText('Note')).toBeInTheDocument();

    // Close button should remove the toast.
    await user.click(screen.getByRole('button', { name: /close/i }));
    expect(screen.queryByText('Saved')).not.toBeInTheDocument();

    // Pushing again and using the action removes it after invoking the handler.
    await user.click(screen.getByRole('button', { name: 'Push' }));
    await user.click(screen.getByRole('button', { name: /undo/i }));
    expect(onAction).toHaveBeenCalledTimes(1);

    // Toast without a title renders the body icon branch.
    await user.click(screen.getByRole('button', { name: 'PushNoTitle' }));
    expect(screen.getByText('Hello')).toBeInTheDocument();
  });
});
