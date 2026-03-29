import React from 'react';
import './Sidebar.css';

export const Sidebar = ({
  isOpen,
  toggleSidebar,
  conversations,
  currentConversationId,
  onSelectConversation,
  onNewChat,
  onDeleteConversation
}) => {
  return (
    <>
      <button
        className={`sidebar-toggle ${isOpen ? 'open' : ''}`}
        onClick={toggleSidebar}
        title="Toggle Sidebar"
      >
        {isOpen ? '◀' : '▶'}
      </button>

      <div className={`sidebar ${isOpen ? 'open' : ''}`}>
        <div className="sidebar-header">
          <h2>Conversations</h2>
          <button className="new-chat-button" onClick={onNewChat}>+ New Chat</button>
        </div>

        <div className="conversations-list">
          {conversations.length === 0 ? (
            <div className="no-conversations">No saved chats yet.</div>
          ) : (
            conversations.map(conv => (
              <div
                key={conv.id}
                className={`conversation-item ${conv.id === currentConversationId ? 'active' : ''}`}
                onClick={() => onSelectConversation(conv.id)}
              >
                <span className="conversation-title">{conv.title}</span>
                <button
                  className="delete-chat-button"
                  onClick={(e) => {
                    e.stopPropagation();
                    onDeleteConversation(conv.id);
                  }}
                  title="Delete Chat"
                >
                  ✕
                </button>
              </div>
            ))
          )}
        </div>
      </div>
    </>
  );
};
