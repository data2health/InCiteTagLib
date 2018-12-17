package edu.uiowa.slis.incite;

public class QueueRequest {
    int ID = 0;
    String url = null;
    
    public QueueRequest(int ID, String url) {
	this.ID = ID;
	this.url = url;
    }
    
    public String toString() {
	return "(" + ID + ") " + url;
    }
}
