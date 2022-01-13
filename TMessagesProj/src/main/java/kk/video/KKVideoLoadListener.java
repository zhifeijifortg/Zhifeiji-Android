package kk.video;

import java.util.List;

/**
 * 读取视频列表结果回调接口
 */
public interface KKVideoLoadListener {

    /**
     * 视频列表结果加载完成后回调
     * @param loadRequestId 请求数据时返回的loadRequestId，用于区分结果
     * @param videoMessages 返回的视频消息列表
     */
    public void onMessagesLoad(int loadRequestId, List<KKVideoMessage> videoMessages);

    /**
     * 视频列表加载出错
     * @param loadRequestId 请求数据是返回的loadRequestId，用于区分结果
     * @param errorCode     错误码
     * @param msg           错误信息
     */
    public void onError(int loadRequestId, int errorCode, String msg);
}
