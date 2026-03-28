import requests

# Point directly to the bridge's native Ollama endpoint
url = "http://127.0.0.1:8000/api/chat"

# Your exact JSON payload
payload = {
    "model": "qwen3:0.6b",  # Or whatever model you are using
    "messages": [
        {
            "role": "user",
            "content": "List all the services"
        }
    ],
    "stream": False
}

try:
    # Send the POST request to the bridge
    response = requests.post(url, json=payload)
    response.raise_for_status()  # Catch any 4xx/5xx errors

    # Parse the Ollama-style response
    result = response.json()
    print("Response Content:")
    print(result.get("message", {}).get("content", "No content found."))

except requests.exceptions.RequestException as e:
    print(f"HTTP Request failed: {e}")
    if e.response is not None:
        print(f"Error details: {e.response.text}")
