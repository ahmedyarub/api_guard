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

      let needNewMessageBubble = true;

      for await (const chunk of stream) {
        const incomingRole = chunk.message?.role || 'assistant';
        const newContent = chunk.message?.content || '';
        const newToolCalls = chunk.message?.tool_calls;

        if (needNewMessageBubble) {
           setMessages((prev) => [
             ...prev,
             { role: incomingRole, content: newContent, tool_calls: newToolCalls }
           ]);
           needNewMessageBubble = false;
        } else {
           setMessages((prev) => {
             const newMessages = [...prev];
             const lastIndex = newMessages.length - 1;
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

        // If the chunk says 'done: true', the stream for the current turn is over.
        // But some bridges (like Ollama's) stream tools, then say 'done', then stream the follow-up
        // answer in the same overarching connection. If another chunk comes after a 'done',
        // it should start a new message bubble.
        if (chunk.done) {
          needNewMessageBubble = true;
        }
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
