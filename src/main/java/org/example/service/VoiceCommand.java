package org.example.service;

import org.example.client.OpenAiClient;
import org.example.dto.chat_gpt.Prompts;
import org.example.dto.chat_gpt.Question;
import org.example.repository.InterviewRepository;
import org.example.repository.TopicRepository;
import org.example.telegram.Bot;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

@Component
public class VoiceCommand extends Command {
    private final Prompts PROMPTS = new Prompts();
    private Deque<Question> answeredQuestions = new ArrayDeque<>();


    public VoiceCommand(OpenAiClient openAiClient,
                        InterviewRepository interviewRepository,
                        TopicRepository topicRepository) {
        super(topicRepository, openAiClient, interviewRepository);
    }

    @Override
    public boolean isApplicable(Update update) {
        return update.getMessage().hasVoice();
    }

    @Override
    public String process(Update update, Bot bot) {
        String answer = transcribeVoiceAnswer(update, bot);
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

    private String transcribeVoiceAnswer(Update update, Bot bot) {
        Voice voice = update.getMessage().getVoice();
        String fileId = voice.getFileId();
        java.io.File audio;
        try {
            GetFile getFileRequest = new GetFile();
            getFileRequest.setFileId(fileId);
            File file = bot.execute(getFileRequest);

            audio = bot.downloadFile(file.getFilePath());
        } catch (TelegramApiException e) {
            throw new IllegalStateException("There's an error when processing Telegram audio", e);
        }
        return openAiClient.transcribe(renameToOgg(audio));
    }

    private java.io.File renameToOgg(java.io.File tmpFile) {
        String fileName = tmpFile.getName();
        String newFileName = fileName.substring(0, fileName.length() - 4) + ".ogg";
        Path sourcePath = tmpFile.toPath();
        Path targetPath = sourcePath.resolveSibling(newFileName);
        try {
            Files.move(sourcePath, targetPath);
        } catch (IOException e) {
            throw new IllegalStateException("There was an error when renaming .tmp audio file to .ogg", e);
        }
        return targetPath.toFile();
    }

    private String askNextQuestion(String userId) {
        Deque<Question> questions = interviewRepository.getUserQuestions().get(userId);

        if (questions == null || questions.isEmpty()) {
            return "All questions were already asked!";
        }

        while (!questions.isEmpty() && questions.peek().getAnswer() != null) {
            questions.poll();
        }

        Question nextQuestion = questions.peek();

        String prompt = String.format(PROMPTS.getQuestionPrompt(), nextQuestion.getQuestion());
        String enrichedQuestion = openAiClient.promptModel(prompt);

        if (enrichedQuestion == null || enrichedQuestion.isEmpty()) {
            throw new IllegalStateException("OpenAI returned empty question!");
        }

        return enrichedQuestion;
    }

    private String provideFeedback(String userId) {
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
