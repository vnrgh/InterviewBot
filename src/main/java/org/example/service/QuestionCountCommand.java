package org.example.service;

import org.example.client.OpenAiClient;
import org.example.dto.chat_gpt.Prompts;
import org.example.dto.chat_gpt.Question;
import org.example.repository.InterviewRepository;
import org.example.repository.TopicRepository;
import org.example.telegram.Bot;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Deque;

@Component
public class QuestionCountCommand extends Command {

    public QuestionCountCommand(TopicRepository topicRepository,
                                OpenAiClient openAiClient,
                                InterviewRepository interviewRepository,
                                InterviewService interviewService) {
        super(topicRepository, openAiClient, interviewRepository, interviewService);
    }

    @Override
    public boolean isApplicable(Update update) {
        String userId = getUserId(update);
        if (!interviewService.isAwaitingQuestionCount(userId)) {
            return false;
        }

        try {
            int count = Integer.parseInt(extractTextOrCallbackData(update));
            return count >= 1 && count <= 10;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String process(Update update, Bot bot) {
        String userId = getUserId(update);
        int count = Integer.parseInt(extractTextOrCallbackData(update));

        interviewService.saveQuestionCount(userId, count);
        interviewRepository.startSession(userId, count);

        Deque<Question> questions = interviewRepository.getUserQuestions().get(userId);
        Question first = questions.peek();
        String prompt = String.format(new Prompts().getQuestionPrompt(), first.getQuestion());
        String enriched = openAiClient.promptModel(prompt);
        return "Интервью начинается! Вот первый вопрос:\n\n" + enriched;
    }

    private String getUserId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getFrom().getId().toString();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getFrom().getId().toString();
        }
        throw new IllegalStateException("Unsupported update type");
    }

    private String extractTextOrCallbackData(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getText();
        } else if(update.hasCallbackQuery()) {
            return update.getCallbackQuery().getData();
        }
        return "";
    }
}
