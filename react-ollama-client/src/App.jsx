import React, { useCallback, useState, useEffect, useMemo } from 'react';
import './App.css';

import { useModels } from './hooks/useModels';
import { useChat } from './hooks/useChat';
import { useConversations } from './hooks/useConversations';

import { ChatHeader, ModelSelector } from './components/ChatHeader';
import { MessageList } from './components/MessageList';
import { MessageInput } from './components/MessageInput';
import { Sidebar } from './components/Sidebar';

function App() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const { models, selectedModel, handleModelChange, error: modelsError } = useModels();

  const {
    conversations,
    currentConversationId,
    setCurrentConversationId,
    createNewChat,
    updateCurrentChatMessages,
    deleteConversation
  } = useConversations();

  const handleMessagesUpdated = useCallback((newMessages) => {
    if (currentConversationId) {
      updateCurrentChatMessages(currentConversationId, newMessages);
    } else if (newMessages.length > 0) {
      // Create a new chat if there isn't one and messages exist
      const newId = createNewChat();
      updateCurrentChatMessages(newId, newMessages);
    }
  }, [currentConversationId, updateCurrentChatMessages, createNewChat]);

  const currentChatMessages = useMemo(() => {
    return currentConversationId
      ? conversations.find(c => c.id === currentConversationId)?.messages || []
      : [];
  }, [conversations, currentConversationId]);

  const { messages, setMessages, isLoading, sendMessage, cancelRequest } = useChat(selectedModel, currentChatMessages, handleMessagesUpdated);

  // When switching conversations, update the chat messages
  useEffect(() => {
    setMessages(currentChatMessages);
  }, [currentConversationId, currentChatMessages, setMessages]);


  // useMemo to prevent re-creating the callback if selectedModel changes, though we pass it directly
  const onModelChange = useCallback((e) => {
    handleModelChange(e.target.value);
  }, [handleModelChange]);

  const handleMessageSubmit = useCallback((text) => {
    sendMessage(text);
  }, [sendMessage]);

  const toggleSidebar = () => setIsSidebarOpen(!isSidebarOpen);

  const handleNewChat = () => {
    createNewChat();
    // if on mobile, might want to close sidebar here
  };

  const handleSelectConversation = (id) => {
    setCurrentConversationId(id);
    // if on mobile, might want to close sidebar here
  };

  return (
    <div className={`app-wrapper ${isSidebarOpen ? 'sidebar-open' : ''}`}>
      <Sidebar
        isOpen={isSidebarOpen}
        toggleSidebar={toggleSidebar}
        conversations={conversations}
        currentConversationId={currentConversationId}
        onSelectConversation={handleSelectConversation}
        onNewChat={handleNewChat}
        onDeleteConversation={deleteConversation}
      />
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
    </div>
  );
}

export default App;
