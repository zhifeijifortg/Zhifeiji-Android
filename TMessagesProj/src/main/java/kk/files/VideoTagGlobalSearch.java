package kk.files;

import org.telegram.tgnet.TLRPC;

public class VideoTagGlobalSearch extends TagGlobalSearch {

    public VideoTagGlobalSearch(KKFileTypes fileType) {
        super(fileType);
    }

    @Override
    protected TLRPC.MessagesFilter getMessagesFilter() {
        return new TLRPC.TL_inputMessagesFilterVideo();
    }
}
