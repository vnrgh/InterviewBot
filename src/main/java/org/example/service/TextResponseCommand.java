package org.example.service;

import org.example.client.OpenAiClient;
import org.example.repository.InterviewRepository;
import org.example.repository.TopicRepository;
import org.example.telegram.Bot;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class TextResponseCommand extends Command {

    public TextResponseCommand(TopicRepository topicRepository,
                               OpenAiClient openAiClient,
                               InterviewRepository interviewRepository,
                               InterviewService interviewService) {
        super(topicRepository, openAiClient, interviewRepository, interviewService);
    }

    @Override
    public boolean isApplicable(Update update) {
        Message message = update.getMessage();
        return message.hasText() && !"/start".equals(message.getText());
    }

    @Override
    public String process(Update update, Bot bot) {
        String answer = update.getMessage().getText();
        String userId = update.getMessage().getFrom().getId().toString();

        interviewRepository.addAnswer(userId, answer);

        if (interviewRepository.getUnansweredQuestionsCount(userId) == 0) {
            return interviewService.generateFeedback(userId);
        } else {
            return interviewService.askNextQuestion(userId);
        }
    }
}