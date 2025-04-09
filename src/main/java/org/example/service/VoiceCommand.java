package org.example.service;

import org.example.client.OpenAiClient;
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

@Component
public class VoiceCommand extends Command {

    public VoiceCommand(OpenAiClient openAiClient,
                        InterviewRepository interviewRepository,
                        TopicRepository topicRepository,
                        InterviewService interviewService) {
        super(topicRepository, openAiClient, interviewRepository, interviewService);
    }

    @Override
    public boolean isApplicable(Update update) {
        return update.getMessage().hasVoice();
    }

    @Override
    public String process(Update update, Bot bot) {
        String answer = transcribeVoiceAnswer(update, bot);
        String userId = update.getMessage().getFrom().getId().toString();

        interviewRepository.addAnswer(userId, answer);

        if (interviewRepository.getUnansweredQuestionsCount(userId) == 0) {
            return interviewService.generateFeedback(userId);
        } else {
            return interviewService.askNextQuestion(userId);
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
}
