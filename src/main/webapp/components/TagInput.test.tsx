'use client';

import { useState } from 'react';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TagInput from './TagInput';

function TagInputHarness(props: Omit<React.ComponentProps<typeof TagInput>, 'tags' | 'onChange'>) {
  const [tags, setTags] = useState<string[]>([]);
  return <TagInput {...props} tags={tags} onChange={setTags} />;
}

describe('TagInput', () => {
  test('adds tags on Enter', async () => {
    const user = userEvent.setup();
    render(<TagInputHarness id="tags" label="Tags" />);

    const input = screen.getByLabelText('Tags');
    await user.type(input, 'hello{enter}');

    expect(screen.getByText('hello')).toBeInTheDocument();
  });

  test('does not add duplicates', async () => {
    const user = userEvent.setup();
    render(<TagInputHarness id="tags" label="Tags" />);

    const input = screen.getByLabelText('Tags');
    await user.type(input, 'hello{enter}');
    await user.type(input, 'hello{enter}');

    expect(screen.getAllByText('hello')).toHaveLength(1);
  });

  test('shows error when maxTags exceeded', async () => {
    const user = userEvent.setup();
    render(<TagInputHarness id="tags" label="Tags" maxTags={1} />);

    const input = screen.getByLabelText('Tags');
    await user.type(input, 'one{enter}');
    await user.type(input, 'two{enter}');

    expect(screen.getByText('Up to 1 tags allowed.')).toBeInTheDocument();
  });

  test('validates tag length', async () => {
    const user = userEvent.setup();
    render(<TagInputHarness id="tags" label="Tags" minTagLength={2} maxTagLength={3} />);

    const input = screen.getByLabelText('Tags');
    await user.type(input, 'a{enter}');
    expect(screen.getByText('Tags must be 2-3 characters.')).toBeInTheDocument();
  });

  test('validates tag pattern with a custom message', async () => {
    const user = userEvent.setup();
    render(<TagInputHarness id="tags" label="Tags" pattern={/^[a-z]+$/} errorMessage="Only lowercase letters allowed." />);

    const input = screen.getByLabelText('Tags');
    await user.type(input, 'ABC{enter}');
    expect(screen.getByText('Only lowercase letters allowed.')).toBeInTheDocument();
  });

  test('splits input on commas', async () => {
    const user = userEvent.setup();
    render(<TagInputHarness id="tags" label="Tags" />);

    const input = screen.getByLabelText('Tags');
    await user.type(input, 'a,b{enter}');

    expect(screen.getByText('a')).toBeInTheDocument();
    expect(screen.getByText('b')).toBeInTheDocument();
  });

  test('removes tags', async () => {
    const user = userEvent.setup();
    render(<TagInputHarness id="tags" label="Tags" />);

    const input = screen.getByLabelText('Tags');
    await user.type(input, 'hello{enter}');

    await user.click(screen.getByLabelText('Remove hello'));
    expect(screen.queryByText('hello')).not.toBeInTheDocument();
  });

  test('loads suggestions with debounce', async () => {
    jest.useFakeTimers();
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });

    const loadSuggestions = jest.fn(async () => ['tag1', 'tag2']);

    render(<TagInputHarness id="tags" label="Tags" loadSuggestions={loadSuggestions} />);

    const input = screen.getByLabelText('Tags');
    await user.type(input, 't');
    // Update the input again to cover debounce cancellation.
    await user.type(input, 'a');

    expect(loadSuggestions).not.toHaveBeenCalled();
    await act(async () => {
      jest.advanceTimersByTime(260);
    });

    expect(loadSuggestions).toHaveBeenCalledWith('ta');
    expect(document.querySelector('datalist option[value="tag1"]')).toBeTruthy();
    expect(document.querySelector('datalist option[value="tag2"]')).toBeTruthy();

    // Clearing the input should clear suggestions.
    await user.clear(input);
    expect(document.querySelectorAll('datalist option').length).toBe(0);

    jest.useRealTimers();
  });
});
