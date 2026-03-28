import React, { memo } from 'react';

export const ModelSelector = memo(({ models, selectedModel, onModelChange, isLoading }) => {
  return (
    <div className="model-selector">
      <label htmlFor="model-select">Model: </label>
      <select
        id="model-select"
        value={selectedModel}
        onChange={onModelChange}
        disabled={isLoading || models.length === 0}
      >
        {models.length === 0 && <option value={selectedModel || 'qwen3:0.6b'}>{selectedModel || 'qwen3:0.6b'}</option>}
        {models.map((model) => (
          <option key={model.name} value={model.name}>
            {model.name}
          </option>
        ))}
      </select>
    </div>
  );
});

export const ChatHeader = memo(({ children }) => {
  return (
    <header className="app-header">
      <h1>Ollama Local Chat</h1>
      {children}
    </header>
  );
});
