import { useState, useCallback, useRef } from 'react';
import { fetchChatApiStream } from '../api/ollama';
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
      const stream = fetchChatApiStream(selectedModel, newConversation, controller.signal);

      let initializedAssistantMessage = false;

      for await (const chunk of stream) {
        if (!initializedAssistantMessage) {
           // We've received the first chunk. Let's add an empty assistant message to the list
           // and then we will update it incrementally.
           setMessages((prev) => [
             ...prev,
             new ChatMessage('assistant', chunk.message?.content || '')
           ]);
           initializedAssistantMessage = true;
        } else {
           // Update the last message in the array
           setMessages((prev) => {
             const newMessages = [...prev];
             const lastIndex = newMessages.length - 1;

             // Tool calls or updates usually don't have 'content' immediately, or append content incrementally
             const newContent = chunk.message?.content || '';
             const newToolCalls = chunk.message?.tool_calls;

             const existingMessage = newMessages[lastIndex];

             newMessages[lastIndex] = {
               ...existingMessage,
               content: existingMessage.content + newContent,
               // Overwrite tool calls if they exist, or keep existing ones
               ...(newToolCalls ? { tool_calls: newToolCalls } : {})
             };

             return newMessages;
           });
        }
      }

      // If stream finishes without ever yielding a message:
      if (!initializedAssistantMessage) {
        setMessages((prev) => [...prev, new ChatMessage('assistant', 'No response received')]);
      }

    } catch (error) {
      if (error.name === 'AbortError') {
        console.log('Request cancelled');
        return;
      }

      console.error('Error connecting to bridge or processing stream:', error);
      setMessages((prev) => [
        ...prev,
        new ChatMessage('assistant', 'Error: Could not process the response. Ensure the MCP bridge is running.'),
      ]);
    } finally {
      if (abortControllerRef.current === controller) {
        setIsLoading(false);
      }
    }
  }, [messages, selectedModel, cancelRequest]);

  return { messages, isLoading, sendMessage, cancelRequest };
};
