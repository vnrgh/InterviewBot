package org.example.service;

import org.example.client.OpenAiClient;
import org.example.dto.chat_gpt.Question;
import org.example.repository.InterviewRepository;
import org.example.repository.TopicRepository;
import org.example.telegram.Bot;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;


@Component
public class StartCommand extends Command {

    private static final String INTERVIEW_PROMPT = """
                        Весело и тепло поприветствуй кандидата на собеседовании, а также расскажи ему о правилах интервью:
                        1. Будет 10 вопросов по Java на уровень Java Junior
                        2. Вопросы будут по Java Core и только по нему
                        3. Отвечать на вопросы кандидат может голосовыми сообщениями и так подробно, как считает нужным
                        4. По итогам его 10 ответов будет предоставлена обратная связь от собеседующего с обозначением
                        сильных сторон, а также мест, где ответы были не очень точны с указанием, на какие аспекты подготовки
                        кандидату нужно обратить внимание

                        Сам следуй правилам выше при формировании вопросов. Учитывай, что кандидат пока может не знать
                        про веб-приложения и сложную разработку. Но он знает про популярные приложения, которые есть
                        в мире, такие как YouTube, Tinder, Twitter или Telegram. Но не используй Meta, Facebook или
                        Instagram в качестве своих примеров. Прочие же приложения могут участвовать в формировании
                        контекста для твоих вопросов, чтобы сделать собеседование более интересным для кандидата.

                        Вот исходный вопрос для собеседования на Java Junior в Java: %s
                        Задай его, придумав интересную ситуацию c каким-нибудь реальным очень известным
                        приложением, в контексте которого и будет задаваться вопрос. Чтобы это не выглядело сухо, как
                        просто вопрос по Java. Подумай, как можно сделать его более интересным и понятным для кандидата
                        с помощью дополнительного контекста реального приложения.

                        Пример: Представим, что мы работаем в Google над приложением YouTube. И нам поручают задачу:
                        создать класс видео, в котором будет 3 поля: название, количество лайков и количество просмотров.
                        Добавляя эти поля, нам важно соблюсти принцип инкапсуляции в ООП. Что это такое, и как он
                        реализуется в Java?

                        Используй и другие примеры известных приложений, не только YouTube. Но иногда можно и YouTube.

                        Стиль общения:
                        Общайся с кандидатом на "ты". Это должна быть беседа двух хороших друзей, тепло и непринужденно,
                        без лишних формальностей.
                        Избегай слишком многословных формулировок. Старайся формулировать предложения четко и по делу,
                        но в то же время сохраняй ламповость и живость беседы. Нужно найти баланс. Тем не менее,
                        человек не должен получить от тебя огромный текст, который ему будет лень читать. Разговор
                        должен быть интересным, но лаконичным, чтобы человек не потерял желание его продолжать из-за
                        огромного количества текста на экране.
                        """;

    public StartCommand(TopicRepository topicRepository,
                        OpenAiClient openAiClient,
                        InterviewRepository interviewRepository,
                        InterviewService interviewService) {
        super(topicRepository, openAiClient, interviewRepository, interviewService);
    }

    @Override
    public boolean isApplicable(Update update) {
        Message message = update.getMessage();
        return message.hasText() && "/start".equals(message.getText());
    }

    @Override
    public String process(Update update, Bot bot) {
        String userId = update.getMessage().getFrom().getId().toString();

        if (!interviewRepository.hasSession(userId)) {
            interviewService.markAwaitingQuestionCount(userId);

            SendMessage message = prepareKeyboardMarkup(update);

            try {
                bot.execute(message);
            } catch (TelegramApiException e) {
                throw new RuntimeException("Ошибка при отправке сообщения с кнопками", e);
            }

            return "Ожидаю выбора количества вопросов...";
        }

        Deque<Question> questions = interviewRepository.getUserQuestions().get(userId);
        if (questions == null || questions.isEmpty()) {
            return "Ошибка: список вопросов пуст или не инициализирован.";
        }

        String topic = questions.peekFirst().getQuestion();
        String prompt = String.format(INTERVIEW_PROMPT, topic);

        Question dto = new Question();
        dto.setQuestion(topic);

        return openAiClient.promptModel(prompt);
    }

    private SendMessage prepareKeyboardMarkup(Update update) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        List<InlineKeyboardButton> buttonList = Arrays.asList(
                createButton("3"), createButton("5"), createButton("10"));

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(buttonList);
        keyboardMarkup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId());
        message.setText("Привет! Перед тем, как начать - из скольких вопросов будет состоять наше интервью? (От 1 до 10). Напиши в чат, либо воспользуйся кнопками ниже: ");
        message.setReplyMarkup(keyboardMarkup);

        return message;
    }

    private InlineKeyboardButton createButton(String text) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(text);
        return button;
    }
}