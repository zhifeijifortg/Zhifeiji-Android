package kk.files;

import org.telegram.tgnet.TLRPC;

public class DocumentTagGlobalSearch extends TagGlobalSearch {

    public DocumentTagGlobalSearch(KKFileTypes fileType) {
        super(fileType);
    }

    @Override
    protected TLRPC.MessagesFilter getMessagesFilter() {
        return new TLRPC.TL_inputMessagesFilterDocument();
    }
}
