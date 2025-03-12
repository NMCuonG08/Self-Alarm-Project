package hcmute.edu.vn.selfalarmproject.model;

import java.io.Serializable;

public class Song implements Serializable {  // Serializable nếu muốn truyền object dễ hơn
    private String name;
    private String url;

    public Song() {
        // Constructor rỗng cho Firebase hoặc Gson parse
    }

    public Song(String name, String url) {
        this.name = name;
        this.url = url;
    }

    // Getter
    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    // Setter
    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // Optional: override toString để debug
    @Override
    public String toString() {
        return "Song{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
