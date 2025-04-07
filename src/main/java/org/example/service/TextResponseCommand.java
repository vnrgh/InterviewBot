package org.example.service;

import org.example.client.OpenAiClient;
import org.example.dto.chat_gpt.Prompts;
import org.example.dto.chat_gpt.Question;
import org.example.repository.InterviewRepository;
import org.example.repository.TopicRepository;
import org.example.telegram.Bot;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayDeque;
import java.util.Deque;

@Component
public class TextResponseCommand extends Command {
    private final Prompts PROMPTS = new Prompts();
    private Deque<Question> answeredQuestions = new ArrayDeque<>();


    public TextResponseCommand(TopicRepository topicRepository,
                               OpenAiClient openAiClient,
                               InterviewRepository interviewRepository) {
        super(topicRepository, openAiClient, interviewRepository);
    }

    @Override
    public boolean isApplicable(Update update) {
        Message message = update.getMessage();
        return message.hasText() && !"/start".equals(message.getText());
    }

    @Override
    public String process(Update update, Bot bot) {
        String answer = update.getMessage().toString();
        String userId = update.getMessage().getFrom().getId().toString();

        if (interviewRepository.isAwaitingQuestionCount(userId)) {
            String text = update.getMessage().getText();
            try {
                int count = Integer.parseInt(text);
                if (count < 1 || count > 10) {
                    return "Введите число от 1 до 10";
                }

                interviewRepository.saveQuestionCount(userId, count);
                interviewRepository.startSession(userId, count);

                String prompt = String.format(PROMPTS.getQuestionPrompt(), interviewRepository.getUserQuestions().get(userId).peek().getQuestion());
                String enrichedQuestion = openAiClient.promptModel(prompt);

                    return "Интервью начинается! Вот первый вопрос:\n\n" +
                            enrichedQuestion;

            } catch (NumberFormatException e) {
                return "Введите корректное число";
            }
        }

        interviewRepository.addAnswer(userId, answer);
        answeredQuestions.add(interviewRepository.getUserQuestions().get(userId).peek());

        if (interviewRepository.getUnansweredQuestionsCount(userId) == 0) {
            return provideFeedback(userId);
        } else {
            return askNextQuestion(userId);
        }
    }

    private String askNextQuestion(String userId) {
        Deque<Question> questions = interviewRepository.getUserQuestions().get(userId);

        if (questions == null || questions.isEmpty()) {
            return "All questions were already asked!";
        }

        while (!questions.isEmpty() && questions.peek().getAnswer() != null) {
            answeredQuestions.add(questions.peek());
            questions.poll();
        }

        Question nextQuestion = questions.peek();

        String prompt = String.format(PROMPTS.getQuestionPrompt(), nextQuestion.getQuestion());
        String enrichedQuestion = openAiClient.promptModel(prompt);

        return enrichedQuestion;
    }

    public String provideFeedback(String userId) {
        StringBuilder feedbackPrompt = new StringBuilder();
        feedbackPrompt.append(PROMPTS.getFeedbackPrompt());

        if (answeredQuestions == null || answeredQuestions.isEmpty()) {
            throw new IllegalStateException("No interview data found for user " + userId);
        }

        answeredQuestions.forEach(question -> feedbackPrompt.append("Исходный вопрос: ")
                .append(question.getQuestion()).append("\n")
                .append("Ответ кандидата: ")
                .append(question.getAnswer()).append("\n"));

        return openAiClient.promptModel(feedbackPrompt.toString());
    }
}

