/**
 * Author: Joshua Wolfel
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Game {
    private String phrase = "";
    private int fail_counter = 0;
    private String board = "";
    private boolean game_over = false;

    Game(List<String> words, int difficulty) {
        this.phrase = cleanPhrase(words);
        this.fail_counter = words.size() * difficulty;
        this.board = phrase.replaceAll("\\w", "-");
        System.out.println(this.board);
    }

    public boolean makeGuess(String guess) {
        String clean_guess = cleanGuess(guess);
        List<String> phrase_words;
        if (guess.length() == 1) {
            return guessLetter(Character.toUpperCase(guess.charAt(0)));
        } else {
            phrase_words = new ArrayList<String>(Arrays.asList(
                clean_guess.split("\\s+")
            ));
            return guessPhrase(cleanPhrase(phrase_words).toUpperCase());
        }
    }

    private String cleanPhrase(List<String> words) {
        StringBuilder b = new StringBuilder();
        words.forEach((word) -> b.append(word + " "));
        return b.toString().trim();
    }
    private String cleanGuess(String guess) {
        return guess.trim();
    }

    private boolean guessLetter(char guess) {
        boolean found_letter = false;
        String updated_board = "";
        char phrase_char;
        char phrase_char_upper;

        for (int i = 0; i < phrase.length(); i+=1) {
            phrase_char = phrase.charAt(i);
            phrase_char_upper = Character.toUpperCase(phrase_char);
            if (phrase_char < 'A' ) {
                updated_board += phrase_char;
            } else if (phrase_char_upper == guess) {
                updated_board += phrase_char;
                found_letter = true;
            } else {
                updated_board += board.charAt(i);
            }
        }

        this.updateGameState(found_letter, updated_board);

        return found_letter;
    }

    public boolean isGameOver() {
        return this.game_over;
    }

    public int getFailCounter() {
        return this.fail_counter;
    }

    public String getBoard() {
        return this.board + "C" + Integer.toString(this.fail_counter);
    }

    private void updateGameState(boolean found_letter, String updated_board) {
        this.board = updated_board;
        if (!found_letter) {
            this.fail_counter -= 1;
        }
        if (this.board.equals(this.phrase) || this.fail_counter < 1) {
            this.game_over = true;
        }

    }

    private boolean guessPhrase(String guess) {
        String updated_board = this.board;
        boolean found_phrase = false;
        int letters_not_found = 1;

        if (this.phrase.toUpperCase().equals(guess.toUpperCase())) {
            found_phrase = true;
            letters_not_found = 0;
            updated_board = this.phrase;
        }

        this.updateGameState(found_phrase, updated_board);

        return found_phrase;
    }

}
