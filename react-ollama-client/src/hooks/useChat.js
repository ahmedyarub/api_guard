import { useState, useCallback, useRef, useEffect } from 'react';
import { fetchChatApiStream } from '../api/ollama';
import { ChatMessage } from '../models/chat';

export const useChat = (selectedModel, initialMessages = [], onMessagesUpdated = null) => {
  const [messages, setMessages] = useState(initialMessages);
  const [isLoading, setIsLoading] = useState(false);
  const abortControllerRef = useRef(null);

  // Sync initialMessages if they change (e.g. loading a different chat)
  // We use useEffect to reset messages when initialMessages reference changes
  // In our case, passing the conversation's messages array is perfect.
  // Actually, standard practice for this is passing a key to the component,
  // but since useChat is a hook, we just need to watch initialMessages.

  const cancelRequest = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      setIsLoading(false);
    }
  }, []);

  const updateAndNotifyRef = useRef(onMessagesUpdated);
  useEffect(() => {
    updateAndNotifyRef.current = onMessagesUpdated;
  }, [onMessagesUpdated]);

  const updateAndNotify = useCallback((newMessagesCallback) => {
    setMessages((prev) => {
      const updatedMessages = typeof newMessagesCallback === 'function' ? newMessagesCallback(prev) : newMessagesCallback;
      if (updateAndNotifyRef.current) {
        updateAndNotifyRef.current(updatedMessages);
      }
      return updatedMessages;
    });
  }, []);


  const sendMessage = useCallback(async (content) => {
    if (!content.trim()) return;

    // Cancel any ongoing request before starting a new one
    cancelRequest();

    const userMessage = new ChatMessage('user', content);

    let newConversation = [];
    setMessages((prev) => {
      newConversation = [...prev, userMessage];
      if (onMessagesUpdated) {
        onMessagesUpdated(newConversation);
      }
      return newConversation;
    });

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
           updateAndNotify((prev) => [
             ...prev,
             { role: incomingRole, content: newContent, tool_calls: newToolCalls }
           ]);
           needNewMessageBubble = false;
        } else {
           updateAndNotify((prev) => {
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
      updateAndNotify((prev) => [
        ...prev,
        new ChatMessage('assistant', 'Error: Could not process the response. Ensure the MCP bridge is running.'),
      ]);
    } finally {
      if (abortControllerRef.current === controller) {
        setIsLoading(false);
      }
    }
  }, [selectedModel, cancelRequest, updateAndNotify, onMessagesUpdated]);

  return { messages, setMessages, isLoading, sendMessage, cancelRequest };
};
