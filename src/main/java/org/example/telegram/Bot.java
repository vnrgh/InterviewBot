package org.example.telegram;

import org.example.client.ElevenLabsClient;
import org.example.service.Command;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.InputFile;
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
        if (update.hasMessage() || update.hasCallbackQuery()) {
            commands.stream()
                    .filter(command -> command.isApplicable(update))
                    .findFirst()
                    .ifPresent(command -> {
                        String answer = command.process(update, this);
                        byte[] audioData = elevenLabsClient.generateSpeech(answer);

                        SendVoice sendVoice = new SendVoice();
                        sendVoice.setChatId(extractChatId(update).toString());
                        sendVoice.setVoice(new InputFile(new ByteArrayInputStream(audioData), "response.ogg"));

                        try {
                            execute(sendVoice);
                        } catch (TelegramApiException e) {
                            throw new IllegalStateException("Error sending voice message", e);
                        }
                    });
        }
    }

    public Long extractChatId(Update update) {
        return update.hasMessage()
                ? update.getMessage().getChatId()
                : update.getCallbackQuery().getMessage().getChatId();
    }

    @Override
    public String getBotUsername() {
        return "your_bot_username";
    }
}
