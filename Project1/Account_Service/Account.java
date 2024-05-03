public class Account {
    
    private String userName;
    private int lifeScore;
    private boolean signedIn;

    public Account( String userName, int lifeScore, boolean signedIn){
        this.userName = userName;
        this.lifeScore = lifeScore;
        this.signedIn = signedIn;
    }

    public String getUserName(){
        return this.userName;
    }

    public int getLifeScore(){
        return this.lifeScore;
    }

    public boolean getSignedIn(){
        return this.signedIn;
    }

    public void setUserName(String username){
        this.userName = username;
        return;
    }

    public void setLifeScore(int score){
        this.lifeScore = score;
        return;
    }

    public void setSignedIn( boolean set ){
        this.signedIn = set;
        return;
    }



}
