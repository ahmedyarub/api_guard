import { useState, useCallback, useRef } from 'react';
import { fetchChatApi } from '../api/ollama';
import { ChatMessage } from '../models/chat';

export const useChat = (selectedModel) => {
  const [messages, setMessages] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const abortControllerRef = useRef(null);

  const cancelRequest = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      setIsLoading(false);
    }
  }, []);

  const sendMessage = useCallback(async (content) => {
    if (!content.trim()) return;

    // Cancel any ongoing request before starting a new one
    cancelRequest();

    const userMessage = new ChatMessage('user', content);
    const newConversation = [...messages, userMessage];

    setMessages(newConversation);
    setIsLoading(true);

    const controller = new AbortController();
    abortControllerRef.current = controller;

    try {
      const data = await fetchChatApi(selectedModel, newConversation, controller.signal);

      const responseMessage = data.message || new ChatMessage('assistant', 'No response received');
      setMessages((prev) => [...prev, responseMessage]);
    } catch (error) {
      if (error.name === 'AbortError') {
        console.log('Request cancelled');
        return; // Don't add an error message if the user intentionally cancelled
      }

      console.error('Error connecting to bridge:', error);
      setMessages((prev) => [
        ...prev,
        new ChatMessage('assistant', 'Error: Could not connect to the bridge. Make sure the MCP bridge is running on port 8000.'),
      ]);
    } finally {
      if (abortControllerRef.current === controller) {
        setIsLoading(false);
      }
    }
  }, [messages, selectedModel, cancelRequest]);

  return { messages, isLoading, sendMessage, cancelRequest };
};
