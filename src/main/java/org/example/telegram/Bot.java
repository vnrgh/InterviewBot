package org.example.telegram;

import org.example.client.ElevenLabsClient;
import org.example.service.Command;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.util.List;

@Component
public class Bot extends TelegramLongPollingBot {

    private final List<Command> commands;
    private final ElevenLabsClient elevenLabsClient;

    public Bot(@Value("${bot.token}") String token,
               List<Command> commands, ElevenLabsClient elevenLabsClient) {
        super(token);
        this.commands = commands;
        this.elevenLabsClient = elevenLabsClient;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            commands.stream()
                    .filter(command -> command.isApplicable(update))
                    .findFirst()
                    .ifPresent(command -> {
                        Message message = update.getMessage();
                        String answer = command.process(update, this);
                        byte[] audioData = elevenLabsClient.generateSpeech(answer);

                        SendVoice sendVoice = new SendVoice();
                        sendVoice.setChatId(message.getChatId().toString());
                        sendVoice.setVoice(new InputFile(new ByteArrayInputStream(audioData), "response.ogg"));

                        try {
                            execute(sendVoice);
                        } catch (TelegramApiException e) {
                            throw new IllegalStateException("Error sending voice message", e);
                        }
                    });
        }
    }

    @Override
    public String getBotUsername() {
        return "your_bot_username";
    }
}
