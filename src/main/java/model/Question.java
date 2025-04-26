package model;

import java.util.List;

//represents a single multi-choice question
public class Question {
    private final String prompt; //question text
    private final List<String> options; //list of answer choices
    private final int correctIndex; //index of answer choices

    public Question(String prompt, List<String> options, int correctIndex) {
        this.prompt = prompt;
        this.options = options;
        this.correctIndex = correctIndex;
    }

    public String getPrompt() {
        return prompt;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCorrectIndex() {
        return correctIndex;
    }
}
