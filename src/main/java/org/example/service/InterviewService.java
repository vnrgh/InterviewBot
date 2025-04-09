package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.client.OpenAiClient;
import org.example.dto.chat_gpt.Prompts;
import org.example.dto.chat_gpt.Question;
import org.example.repository.InterviewRepository;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InterviewService {
    private final InterviewRepository interviewRepository;
    private final OpenAiClient openAiClient;
    private final Prompts PROMPTS = new Prompts();
    private final Map<String, Boolean> awaitingQuestionCount = new HashMap<>();
    private final Map<String, Integer> questionCounts = new HashMap<>();

    public String askNextQuestion(String userId) {
        Deque<Question> questions = interviewRepository.getUserQuestions().get(userId);
        if (questions == null || questions.isEmpty()) {
            return null;
        }

        Question nextQuestion = questions.stream()
                .filter(q -> q.getAnswer() == null)
                .findFirst()
                .orElse(null);

        if (nextQuestion == null) {
            return "All questions were already asked!";
        }

        String prompt = String.format(PROMPTS.getQuestionPrompt(), nextQuestion.getQuestion());
        return openAiClient.promptModel(prompt);
    }

    public String generateFeedback(String userId) {
        List<Question> answered = interviewRepository.getAnsweredQuestions(userId);
        if (answered == null || answered.isEmpty()) return "Нет вопросов для оценки.";

        StringBuilder feedbackPrompt = new StringBuilder();
        feedbackPrompt.append(PROMPTS.getFeedbackPrompt());

        answered.forEach(question -> feedbackPrompt.append("Исходный вопрос: ")
                .append(question.getQuestion()).append("\n")
                .append("Ответ кандидата: ")
                .append(question.getAnswer()).append("\n"));

        return openAiClient.promptModel(feedbackPrompt.toString());
    }

    public void markAwaitingQuestionCount(String userId) {
        awaitingQuestionCount.put(userId, true);
    }

    public boolean isAwaitingQuestionCount(String userId) {
        return awaitingQuestionCount.getOrDefault(userId, false);
    }

    public void saveQuestionCount(String userId, int count) {
        questionCounts.put(userId, count);
        awaitingQuestionCount.remove(userId);
    }
}