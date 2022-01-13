package kk.event;

/**
 * Created by LSD on 2021/3/20.
 * Desc
 */
public class DeleteVideoItemEvent {
    public String filePath;

    public DeleteVideoItemEvent(String filePath) {
        this.filePath = filePath;
    }
}
