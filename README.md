# InterviewBot — Telegram Bot for Java Junior Interview Preparation

**InterviewBot** is a Telegram bot that helps you prepare for Java Junior interviews.  
It asks questions using the ChatGPT API, evaluates your answers, and provides feedback at the end of the session.  
You can interact with the bot using both **text and voice messages** — thanks to the integration with **ElevenLabs** and **ChatGPT** APIs.

**Bot contains prompts only in russian language. Keep this in mind.**

---

## Features

- Generates Java interview questions using ChatGPT
- Accepts user responses in text form
- Supports voice questions and voice answers
- Provides summarized feedback at the end of the session
- Simple interaction through Telegram

---

## Technologies

- **Java + Spring Boot**
- **Gradle**
- **ChatGPT API** — for question generation and answer evaluation
- **ElevenLabs API** — for voice message synthesis
- **Telegram Bot API** — for message handling

---

## Installation & Running

1. Configure `application.yml`:

   ```yaml
   bot:
      token: YOUR_TELEGRAM_BOT_TOKEN

   openai:
      api:
        key: YOUR_OPENAI_API_KEY

   eleven_labs:
      api:
        key: YOUR_ELEVENLABS_API_KEY
   ```

2. Set your bot name in `Bot.java`:

   ```java
   @Override
   public String getBotUsername() {
       return "your_bot_name"; // e.g. "InterviewJavaBot"
   }
   ```

3. Build and run the project:

   ```bash
   ./gradlew bootRun
   ```

The bot runs with **TelegramLongPollingBot**, so no webhook setup is required. Just start the app and the bot will begin processing incoming messages.

---

## API Keys

| API             | Where to Get It                             |
|------------------|---------------------------------------------|
| Telegram Bot     | [@BotFather on Telegram](https://t.me/BotFather) |
| OpenAI (ChatGPT) | https://platform.openai.com/account/api-keys |
| ElevenLabs       | https://www.elevenlabs.io/api               |

---

## Contact

Feel free to reach out for ideas or improvements!

Project author: https://github.com/vnrgh

