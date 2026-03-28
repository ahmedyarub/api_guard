import { test, expect } from '@playwright/test';

test('renders markdown response from MCP bridge', async ({ page }) => {
  // 1. Mock the /api/tags endpoint to provide the default model
  await page.route('/api/tags', async route => {
    const json = {
      models: [
        { name: "qwen3:0.6b" },
        { name: "llama3.2" }
      ]
    };
    await route.fulfill({ json });
  });

  // 2. Mock the /api/chat endpoint to intercept the message and return the exact JSON.
  // Note: Vite proxies this, but Playwright intercepts requests made *from the browser*
  // so we intercept the relative URL '/api/chat'
  await page.route('**/api/chat', async route => {
    const request = route.request();
    const postData = request.postDataJSON();

    // Verify it's the expected chat request
    if (postData && postData.messages && postData.messages.length > 0) {
      const lastMessage = postData.messages[postData.messages.length - 1];
      if (lastMessage.content === "List the services") {
        // Return the exact JSON payload the user requested
        const json = {
          "model":"qwen3:0.6b",
          "created_at":"2026-03-28T15:44:38.1313082Z",
          "message": {
            "role":"assistant",
            "content":"The list of services shows one service: **caller_feign_client**. Let me know if you need more details!",
            "thinking":"Okay, the user asked to list services, and I used the list_services function. The response from the tool was \"caller_feign_client\". Wait, but the function should return a list, not just one service. Maybe the tool's response was incomplete or there's a misunderstanding. Let me check the function's description again. The list_services function returns a list of all microservice artifact IDs. So if the tool's response is just one entry, that's probably because there's only one service listed. I should inform the user that there's one service available and maybe ask if they need more details. But since the user's previous queries were about services, perhaps they want to know all services. But the tool's response was only \"caller_feign_client\". Maybe the user expects a list, but the tool only returns one. I should clarify and provide that information.\n"
          },
          "done":true,
          "done_reason":"stop",
          "total_duration":875517800,
          "load_duration":108777600,
          "prompt_eval_count":310,
          "prompt_eval_duration":9511300,
          "eval_count":205,
          "eval_duration":707333300
        };
        await route.fulfill({ json });
        return;
      }
    }
    // Fallback for any other request
    await route.continue();
  });

  // 3. Navigate to the application
  // The port depends on your playwright.config.js webServer settings or manual startup
  await page.goto('http://localhost:5173');

  // Verify the dropdown populated with our mocked tags
  await expect(page.locator('#model-select')).toHaveValue('qwen3:0.6b');

  // 4. Send the message
  await page.fill('.input-form input', 'List the services');
  await page.click('button[type="submit"]');

  // 5. Assert the markdown rendered properly
  // The string contains "**caller_feign_client**", so react-markdown should wrap it in a <strong> tag.
  const assistantMessage = page.locator('.message.assistant .message-content p').last();
  await expect(assistantMessage).toContainText('The list of services shows one service: caller_feign_client. Let me know if you need more details!');

  // Specifically assert the bolding was parsed
  const boldElement = assistantMessage.locator('strong');
  await expect(boldElement).toBeVisible();
  await expect(boldElement).toHaveText('caller_feign_client');
});
