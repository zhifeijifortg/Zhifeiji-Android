package kk.files;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

public abstract class TagGlobalSearch extends TagSearch {

    public TagGlobalSearch(KKFileTypes fileType) {
        super(fileType);
    }

    protected TLObject getSearchRequest() {
        final TLRPC.TL_messages_searchGlobal req = new TLRPC.TL_messages_searchGlobal();
        req.limit = 100;
        req.q = fileType.getQueryString();
        req.filter = getMessagesFilter();
        if (lastMessage == null) {
            req.offset_rate = 0;
            req.offset_id = 0;
            req.offset_peer = new TLRPC.TL_inputPeerEmpty();
        } else {
            req.offset_id = lastMessage.getId();
            req.offset_rate = lastMessage.messageOwner.date;
            int id;
            if (lastMessage.messageOwner.peer_id.channel_id != 0) {
                id = -lastMessage.messageOwner.peer_id.channel_id;
            } else if (lastMessage.messageOwner.peer_id.chat_id != 0) {
                id = -lastMessage.messageOwner.peer_id.chat_id;
            } else {
                id = lastMessage.messageOwner.peer_id.user_id;
            }
            req.offset_peer = MessagesController.getInstance(UserConfig.selectedAccount).getInputPeer(id);
        }
        req.flags |= 1;
        return req;
    }
}
