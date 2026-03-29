import React, { useState, memo, useCallback } from 'react';

export const MessageInput = memo(({ onSubmit, isLoading, onCancel }) => {
  const [input, setInput] = useState('');

  const handleSubmit = useCallback((e) => {
    e.preventDefault();
    if (!input.trim() || isLoading) return;

    onSubmit(input);
    setInput('');
  }, [input, isLoading, onSubmit]);

  const handleInputChange = useCallback((e) => {
    setInput(e.target.value);
  }, []);

  return (
    <form onSubmit={handleSubmit} className="input-form">
      <input
        type="text"
        value={input}
        onChange={handleInputChange}
        placeholder="Type your message..."
        disabled={isLoading}
      />
      {isLoading ? (
        <button type="button" onClick={onCancel} className="cancel-button">
          Cancel
        </button>
      ) : (
        <button type="submit" disabled={!input.trim()}>
          Send
        </button>
      )}
    </form>
  );
});
