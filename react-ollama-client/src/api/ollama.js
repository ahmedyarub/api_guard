import { ChatRequest } from '../models/chat';

export const fetchModelsApi = async () => {
  const response = await fetch('/api/tags');
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  return response.json();
};

export const fetchChatApi = async (selectedModel, currentMessages, signal) => {
  const requestPayload = new ChatRequest(selectedModel || 'qwen3:0.6b', currentMessages);

  const response = await fetch('/api/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(requestPayload),
    signal, // Pass the AbortSignal to enable request cancellation
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  return response.json();
};
