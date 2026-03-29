import { useState, useCallback } from 'react';

const STORAGE_KEY = 'ollama_conversations';

export const useConversations = () => {
  const [conversations, setConversations] = useState(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        return JSON.parse(stored);
      } catch (e) {
        console.error("Failed to parse conversations", e);
        return [];
      }
    }
    return [];
  });
  const [currentConversationId, setCurrentConversationId] = useState(null);

  const createNewChat = useCallback(() => {
    const id = Date.now().toString();
    const date = new Date().toLocaleString();
    const newChat = {
      id,
      title: `Chat - ${date}`,
      messages: []
    };

    setConversations((prev) => {
      const updated = [newChat, ...prev];
      localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
      return updated;
    });

    setCurrentConversationId(id);
    return id;
  }, []);

  const updateCurrentChatMessages = useCallback((id, messages) => {
    setConversations((prev) => {
      const updated = prev.map(c => {
        if (c.id === id) {
          // Only update if messages actually changed
          if (JSON.stringify(c.messages) !== JSON.stringify(messages)) {
            return { ...c, messages };
          }
        }
        return c;
      });
      localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
      return updated;
    });
  }, []);

  const deleteConversation = useCallback((id) => {
    setConversations((prev) => {
      const updated = prev.filter(c => c.id !== id);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
      return updated;
    });

    if (currentConversationId === id) {
      setCurrentConversationId(null);
    }
  }, [currentConversationId]);

  return {
    conversations,
    currentConversationId,
    setCurrentConversationId,
    createNewChat,
    updateCurrentChatMessages,
    deleteConversation
  };
};