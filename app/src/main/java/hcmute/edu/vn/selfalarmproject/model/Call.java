package hcmute.edu.vn.selfalarmproject.model;

import java.util.Date;

public class Call {
    public static final int INCOMING_TYPE = 1;
    public static final int OUTGOING_TYPE = 2;
    public static final int MISSED_TYPE = 3;

    private String id;
    private String name;
    private String number;
    private Date timestamp;
    private int type;
    private int duration;

    public Call(String id, String number, String name, Date timestamp, int type, int duration) {
        this.id = id;
        this.number = number;
        this.name = name;
        this.timestamp = timestamp;
        this.type = type;
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}