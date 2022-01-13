package kk.files;

public class TagDialogSearchFactory {

    public static TagSearch getTagDialogSearch(KKFileTypes type, int dialogId) {
        switch (type) {
            case ALL:
            case IGNORE:
            case UNKNOWN:
            case TAG_OTHERS:
            case TAG_VIDEO:
            case TAG_FILES:
                throw new IllegalArgumentException("No TagGlobalSearch for " + type);
            case VIDEO_FILTER:
                return new VideoTagDialogSearch(type, dialogId);
            case APK:
            case DOC:
            case MKV:
            case MOV:
            case MP4:
            case TXT:
                return new DocumentTagDialogSearch(type, dialogId);
            default:
                throw new IllegalStateException();
        }
    }
}
