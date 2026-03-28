import React, { memo } from 'react';
import ReactMarkdown from 'react-markdown';

export const MessageItem = memo(({ msg }) => {
  // Hide raw tool execution messages from the UI for a cleaner chat experience
  if (msg.role === 'tool') return null;

  if (msg.tool_calls) {
    return (
      <div className="message assistant loading">
        <div className="message-content">Calling tool: {msg.tool_calls.map(tc => tc.function.name).join(", ")}...</div>
      </div>
    );
  }

  return (
    <div className={`message ${msg.role}`}>
      <div className="message-content">
        {msg.role === 'user' ? (
          msg.content
        ) : (
          <ReactMarkdown>{msg.content}</ReactMarkdown>
        )}
      </div>
    </div>
  );
});

export const MessageList = memo(({ messages, isLoading }) => {
  return (
    <div className="messages">
      {messages.length === 0 && (
        <div className="empty-state">Start a conversation with Ollama!</div>
      )}
      {messages.map((msg, index) => (
        <MessageItem key={index} msg={msg} />
      ))}
      {isLoading && (
        <div className="message assistant loading">
          <div className="message-content">Thinking...</div>
        </div>
      )}
    </div>
  );
});
