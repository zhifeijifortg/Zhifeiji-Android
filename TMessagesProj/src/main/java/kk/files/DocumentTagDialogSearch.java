package kk.files;

import org.telegram.tgnet.TLRPC;

public class DocumentTagDialogSearch extends TagDialogSearch {

    public DocumentTagDialogSearch(KKFileTypes fileType, int dialogId) {
        super(fileType, dialogId);
    }

    @Override
    protected TLRPC.MessagesFilter getMessagesFilter() {
        return new TLRPC.TL_inputMessagesFilterDocument();
    }
}
