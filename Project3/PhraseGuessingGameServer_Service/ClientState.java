/**
 * Author: Joshua Wolfel
 */
import java.util.concurrent.locks.ReentrantLock;

public class ClientState {
  private User client_info = null;
  private Game client_game = null;
  private int sequence_num = 0;
  private boolean active = true;
  private String cached_response = "";
  private ReentrantLock client_lock = new ReentrantLock();

  ClientState(User client_info) {
    this.client_info = client_info;
  }

  public User getUser() {
    return this.client_info;
  }

  public Game getGame() {
    return this.client_game;
  }

  public void updateGame(Game new_game) {
    this.client_game = new_game;
  }

  public ReentrantLock getLock() {
    return client_lock;
  }

  public void lock() {
    this.client_lock.lock();
  }

  public void unlock() {
    this.client_lock.unlock();
  }

  public boolean isActive(){
    return this.active;
  }

  public void setUnactive() {
    this.active = false;
  }

  public void setActive() {
    this.active = true;
  }

  public void setSequenceNum(int num) {
    this.sequence_num = num;
  }

  public void increaseSequenceNum() {
    this.sequence_num += 1;
  }

  public int getSequenceNum() {
    return this.sequence_num;
  }
  
  public void setCachedResponse(String cached_response) {
    this.cached_response = cached_response;
  }

  public String getCachedResponse() {
    return this.cached_response;
  }

}