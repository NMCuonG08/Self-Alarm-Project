package hcmute.edu.vn.selfalarmproject.model;

import java.util.Date;

public class CallLogData {
    private long id;
    private String number;
    private int type; // 1 = Incoming, 2 = Outgoing, 3 = Missed
    private long date;
    private long duration;
    private String name;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeAsString() {
        switch (type) {
            case 1: return "Cuộc gọi đến";
            case 2: return "Cuộc gọi đi";
            case 3: return "Cuộc gọi nhỡ";
            default: return "Không xác định";
        }
    }

    public String getDateAsString() {
        return new Date(date).toString();
    }

    public String getDurationAsString() {
        long seconds = duration % 60;
        long minutes = (duration / 60) % 60;
        long hours = duration / 3600;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}