import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import App from './App';

// Mock fetch
global.fetch = vi.fn();

describe('App Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders correctly in initial state', () => {
    render(<App />);
    expect(screen.getByText('Ollama Local Chat')).toBeInTheDocument();
    expect(screen.getByText('Start a conversation with Ollama!')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Type your message...')).toBeInTheDocument();
  });

  it('updates input value on change', () => {
    render(<App />);
    const input = screen.getByPlaceholderText('Type your message...');
    fireEvent.change(input, { target: { value: 'Hello' } });
    expect(input.value).toBe('Hello');
  });

  it('sends a message and displays the response', async () => {
    const mockResponse = {
      message: { role: 'assistant', content: 'Hello! I am Llama.' }
    };
    
    fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => mockResponse,
    });

    render(<App />);
    const input = screen.getByPlaceholderText('Type your message...');
    const sendButton = screen.getByRole('button', { name: /send/i });

    fireEvent.change(input, { target: { value: 'Hi' } });
    fireEvent.click(sendButton);

    // Check if user message is added
    expect(screen.getByText('Hi')).toBeInTheDocument();
    
    // Check if loading state appears
    expect(screen.getByText('Thinking...')).toBeInTheDocument();

    // Wait for the assistant response
    await waitFor(() => {
      expect(screen.getByText('Hello! I am Llama.')).toBeInTheDocument();
    });

    // Check if loading state is gone
    expect(screen.queryByText('Thinking...')).not.toBeInTheDocument();
  });

  it('displays error message when API call fails', async () => {
    fetch.mockRejectedValueOnce(new Error('API failure'));

    render(<App />);
    const input = screen.getByPlaceholderText('Type your message...');
    const sendButton = screen.getByRole('button', { name: /send/i });

    fireEvent.change(input, { target: { value: 'Hi' } });
    fireEvent.click(sendButton);

    await waitFor(() => {
      expect(screen.getByText(/Error: Could not connect to Ollama/)).toBeInTheDocument();
    });
  });

  it('disables input and button during loading', async () => {
    fetch.mockReturnValue(new Promise(() => {})); // Never resolves to keep it loading

    render(<App />);
    const input = screen.getByPlaceholderText('Type your message...');
    const sendButton = screen.getByRole('button', { name: /send/i });

    fireEvent.change(input, { target: { value: 'Hi' } });
    fireEvent.click(sendButton);

    expect(input).toBeDisabled();
    expect(sendButton).toBeDisabled();
  });
});
