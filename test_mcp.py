from openai import OpenAI

# Point the client to the ollama-mcp-bridge, not Ollama directly
client = OpenAI(
    base_url="http://localhost:8000/v1",
    api_key="ollama" # The bridge doesn't check the key, but the client requires the field
)

response = client.chat.completions.create(
    model="llama3.2", # Replace with your specific tool-calling model
    messages=[
        {"role": "user", "content": "Analyze the dependencies in the microservices directory."}
    ]
)

print(response.choices[0].message.content)