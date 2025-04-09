package org.example.repository;

import lombok.Getter;
import org.example.dto.chat_gpt.Question;
import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@Getter
public class InterviewRepository {
    private final Map<String, Deque<Question>> userQuestions = new HashMap<>();
    private final TopicRepository topicRepository;

    public InterviewRepository(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    public void startSession(String userId, int questionCount) {
        userQuestions.put(userId, new ArrayDeque<>());

        for (int i = 0; i < questionCount; i++) {
            String question = topicRepository.getRandomTopic();
            Question dto = new Question();
            dto.setQuestion(question);
            userQuestions.get(userId).offer(dto);
        }
    }

    public boolean hasSession(String userId) {
        return userQuestions.containsKey(userId);
    }

    public void addAnswer(String userId, String answer) {
        if (!hasSession(userId)) {
            throw new IllegalStateException("There is no interview session started for user " + userId);
        }
        Deque<Question> questions = userQuestions.get(userId);

        if (questions.isEmpty()) {
            throw new IllegalStateException("No questions available for user " + userId);
        }

        // Ищем первый неотвеченный вопрос
        Question question = questions.stream()
                .filter(q -> q.getAnswer() == null)
                .findFirst()
                .orElse(null);

        if (question == null) {
            throw new IllegalStateException("An answer is already assigned to the last question for user " + userId);
        }

        question.setAnswer(answer);
    }

    public int getUnansweredQuestionsCount(String userId) {
        return (int) userQuestions.getOrDefault(userId, new LinkedList<>())
                .stream()
                .filter(q -> q.getAnswer() == null)
                .count();
    }

    public List<Question> getAnsweredQuestions(String userId) {
        return userQuestions.getOrDefault(userId, new ArrayDeque<>())
                .stream()
                .filter(q -> q.getAnswer() != null)
                .collect(Collectors.toList());
    }
}
