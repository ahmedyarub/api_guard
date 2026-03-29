export class ChatRequest {
  constructor(model, messages, stream = true) {
    this.model = model;
    this.messages = messages;
    this.stream = stream;
  }
}

export class ChatMessage {
  constructor(role, content) {
    this.role = role;
    this.content = content;
  }
}
