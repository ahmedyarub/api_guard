import { ChatRequest } from '../models/chat';

export const fetchModelsApi = async () => {
  const response = await fetch('/api/tags');
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  return response.json();
};

export const fetchChatApiStream = async function* (selectedModel, currentMessages, signal) {
  const requestPayload = new ChatRequest(selectedModel || 'qwen3:0.6b', currentMessages, true);

  const response = await fetch('/api/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(requestPayload),
    signal,
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  if (!response.body) {
    throw new Error('ReadableStream not yet supported in this browser.');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });

      const lines = buffer.split('\n');
      buffer = lines.pop() || ''; // Keep the last incomplete line in the buffer

      for (const line of lines) {
        if (line.trim() === '') continue;

        try {
          const parsed = JSON.parse(line);
          yield parsed;
        } catch (e) {
          console.warn('Failed to parse streaming JSON chunk:', line, e);
          // Could be incomplete JSON if not properly line-delimited from backend, but usually it is line-delimited
        }
      }
    }

    // Process any remaining buffer
    if (buffer.trim() !== '') {
      try {
        const parsed = JSON.parse(buffer);
        yield parsed;
      } catch (e) {
        console.warn('Failed to parse final streaming JSON chunk:', buffer, e);
      }
    }
  } finally {
    reader.releaseLock();
  }
};
