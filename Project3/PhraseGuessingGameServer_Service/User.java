/** 
 * Author Joshua Wolfel
 */
public class User {
    private String username;
    private int score = 0;

    User(String username, int score) {
        this.username = username;
        this.score = score;
    }

    public void updateScore(int points) {
        this.score += points;
    }

    public int getScore() {
        return this.score;
    }

    public String getUsername() {
        return this.username;
    }
}
