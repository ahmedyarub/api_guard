import { useState } from 'react';
import './App.css';

class ChatRequest {
  constructor(model, messages, stream = false) {
    this.model = model;
    this.messages = messages;
    this.stream = stream;
  }
}

class ChatMessage {
  constructor(role, content) {
    this.role = role;
    this.content = content;
  }
}

function App() {
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState([]);
  const [isLoading, setIsLoading] = useState(false);

  const handleChatRequest = async (currentMessages) => {
    // We send requests to the proxy endpoint (/api/chat) which routes to the HTTP bridge
    const requestPayload = new ChatRequest('qwen3:0.6b', currentMessages);

    const response = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestPayload),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!input.trim()) return;

    const userMessage = new ChatMessage('user', input);
    let currentConversation = [...messages, userMessage];
    setMessages(currentConversation);
    setInput('');
    setIsLoading(true);

    try {
      const data = await handleChatRequest(currentConversation);
      const responseMessage = data.message || { role: 'assistant', content: 'No response received' };
      setMessages((prev) => [...prev, responseMessage]);

    } catch (error) {
      console.error('Error connecting to bridge:', error);
      setMessages((prev) => [
        ...prev,
        new ChatMessage('assistant', 'Error: Could not connect to the bridge. Make sure the MCP bridge is running on port 8000.'),
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="app-container">
      <header className="app-header">
        <h1>Ollama Local Chat</h1>
        <p>Model: qwen3:0.6b</p>
      </header>

      <main className="chat-container">
        <div className="messages">
          {messages.length === 0 && (
            <div className="empty-state">Start a conversation with Ollama!</div>
          )}
          {messages.map((msg, index) => {
             // Hide raw tool execution messages from the UI for a cleaner chat experience
             if (msg.role === 'tool') return null;
             if (msg.tool_calls) {
               return (
                 <div key={index} className="message assistant loading">
                   <div className="message-content">Calling tool: {msg.tool_calls.map(tc => tc.function.name).join(", ")}...</div>
                 </div>
               );
             }

             return (
              <div key={index} className={`message ${msg.role}`}>
                <div className="message-content">{msg.content}</div>
              </div>
            );
          })}
          {isLoading && (
            <div className="message assistant loading">
              <div className="message-content">Thinking...</div>
            </div>
          )}
        </div>

        <form onSubmit={handleSubmit} className="input-form">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Type your message..."
            disabled={isLoading}
          />
          <button type="submit" disabled={isLoading || !input.trim()}>
            Send
          </button>
        </form>
      </main>
    </div>
  );
}

export default App;