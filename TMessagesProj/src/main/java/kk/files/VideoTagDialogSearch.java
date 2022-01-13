package kk.files;

import org.telegram.tgnet.TLRPC;

public class VideoTagDialogSearch extends TagDialogSearch {

    public VideoTagDialogSearch(KKFileTypes fileType, int dialogId) {
        super(fileType, dialogId);
    }

    @Override
    protected TLRPC.MessagesFilter getMessagesFilter() {
        return new TLRPC.TL_inputMessagesFilterVideo();
    }
}
