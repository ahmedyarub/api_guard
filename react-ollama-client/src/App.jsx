import React, { useCallback, useMemo } from 'react';
import './App.css';

import { useModels } from './hooks/useModels';
import { useChat } from './hooks/useChat';

import { ChatHeader, ModelSelector } from './components/ChatHeader';
import { MessageList } from './components/MessageList';
import { MessageInput } from './components/MessageInput';

function App() {
  const { models, selectedModel, handleModelChange, error: modelsError } = useModels();
  const { messages, isLoading, sendMessage, cancelRequest } = useChat(selectedModel);

  // useMemo to prevent re-creating the callback if selectedModel changes, though we pass it directly
  const onModelChange = useCallback((e) => {
    handleModelChange(e.target.value);
  }, [handleModelChange]);

  const handleMessageSubmit = useCallback((text) => {
    sendMessage(text);
  }, [sendMessage]);

  return (
    <div className="app-container">
      <ChatHeader>
        <ModelSelector
          models={models}
          selectedModel={selectedModel}
          onModelChange={onModelChange}
          isLoading={isLoading}
        />
      </ChatHeader>

      <main className="chat-container">
        {modelsError && <div className="error-banner">Failed to load models. Using default.</div>}
        <MessageList messages={messages} isLoading={isLoading} />

        <MessageInput
          onSubmit={handleMessageSubmit}
          isLoading={isLoading}
          onCancel={cancelRequest}
        />
      </main>
    </div>
  );
}

export default App;
