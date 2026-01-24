'use client';

import { useEffect, useMemo, useRef, useState } from 'react';
import Form from 'react-bootstrap/Form';
import Badge from 'react-bootstrap/Badge';
import Button from 'react-bootstrap/Button';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faXmark } from '@fortawesome/free-solid-svg-icons';

const defaultPattern = /^[A-Za-z0-9_-]{1,30}$/;

export default function TagInput({
  id,
  label,
  tags,
  onChange,
  loadSuggestions,
  maxTags = 5,
  minTagLength = 1,
  maxTagLength = 30,
  placeholder = 'Type and press Enter or comma',
  disabled = false,
  pattern = defaultPattern,
  helperText,
  errorMessage,
  isInvalid = false,
  externalError,
}) {
  const [inputValue, setInputValue] = useState('');
  const [error, setError] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const debounceRef = useRef(null);
  const datalistId = `${id}-suggestions`;

  useEffect(() => {
    if (!loadSuggestions) return undefined;
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }
    const query = inputValue.trim();
    if (!query) {
      return undefined;
    }
    debounceRef.current = setTimeout(() => {
      loadSuggestions(query)
        .then(items => setSuggestions(Array.isArray(items) ? items : []))
        .catch(() => setSuggestions([]));
    }, 250);
    return () => clearTimeout(debounceRef.current);
  }, [inputValue, loadSuggestions]);

  const normalizedTags = useMemo(() => tags || [], [tags]);
  const combinedError = externalError || error;
  const hasError = Boolean(combinedError) || isInvalid;

  const updateTags = nextTags => {
    onChange?.(nextTags);
    setError('');
  };

  const addTag = raw => {
    const chunk = raw.trim();
    if (!chunk) return;
    if (normalizedTags.length >= maxTags) {
      setError(`Up to ${maxTags} tags allowed.`);
      return;
    }
    if (chunk.length < minTagLength || chunk.length > maxTagLength) {
      setError(`Tags must be ${minTagLength}-${maxTagLength} characters.`);
      return;
    }
    if (!pattern.test(chunk)) {
      setError(errorMessage || 'Tags must use letters, digits, hyphen, or underscore.');
      return;
    }
    if (normalizedTags.includes(chunk)) {
      setInputValue('');
      setSuggestions([]);
      return;
    }
    updateTags([...normalizedTags, chunk]);
    setInputValue('');
    setSuggestions([]);
  };

  const handleKeyDown = event => {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      const parts = inputValue.split(',');
      parts.forEach(part => addTag(part));
    }
  };

  const removeTag = value => {
    updateTags(normalizedTags.filter(tag => tag !== value));
  };

  return (
    <div className="mb-3">
      {label ? (
        <Form.Label htmlFor={id} className="form-label">
          {label}
        </Form.Label>
      ) : null}
      <div className={`form-control p-2 ${hasError ? 'is-invalid' : ''}`}>
        <div className="d-flex flex-wrap gap-2 mb-2">
          {normalizedTags.map(tag => (
            <Badge key={tag} bg="secondary-subtle" text="secondary" className="d-inline-flex align-items-center gap-1">
              <span>{tag}</span>
              <Button
                variant="link"
                size="sm"
                className="p-0 text-secondary"
                onClick={() => removeTag(tag)}
                disabled={disabled}
                aria-label={`Remove ${tag}`}
              >
                <FontAwesomeIcon icon={faXmark} />
              </Button>
            </Badge>
          ))}
        </div>
        <Form.Control
          id={id}
          type="text"
          className="border-0 shadow-none p-0"
          placeholder={placeholder}
          value={inputValue}
          onChange={event => {
            const nextValue = event.target.value;
            setInputValue(nextValue);
            if (!nextValue.trim()) {
              setSuggestions([]);
            }
          }}
          onKeyDown={handleKeyDown}
          list={datalistId}
          disabled={disabled}
        />
        <datalist id={datalistId}>
          {suggestions.map(suggestion => (
            <option key={suggestion} value={suggestion} />
          ))}
        </datalist>
      </div>
      {helperText ? <div className="form-text">{helperText}</div> : null}
      {combinedError ? <div className="invalid-feedback d-block">{combinedError}</div> : null}
    </div>
  );
}
