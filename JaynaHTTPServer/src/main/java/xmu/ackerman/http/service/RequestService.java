package xmu.ackerman.http.service;

import xmu.ackerman.http.context.Context;
import xmu.ackerman.utils.ParseRequestUtil;
import xmu.ackerman.utils.RequestParseState;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午4:12 18-3-15
 */
public class RequestService {
    private static int MAX_BUF = 1024;


    /**
    * @Description: 从通道中获取数据, 考虑到不能一次全部获取的情况, 测试未通过
     *                 不能在这边循环 否则主线程会卡住
    * @Date: 上午10:52 18-3-16
    */
    public static RequestParseState recvFrom(Context context){
        SelectionKey key = context.getSelectionKey();

        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(MAX_BUF);
        RequestParseState state;
        try{
            // parse_more 1.半包数据  2.缓冲区太小
            //需要设置 等待超时时间
            //可能 读取在等待更多的数据 进行parse_more
            int cnt = client.read(buffer);

            if(cnt < 0){
                return requestError(key);
            }
            else if(cnt == 0){
                return RequestParseState.PARSE_MORE;
            }
            byte [] bytes = buffer.array();

            state = ParseRequestUtil.parseHttpRequestLine(context, bytes);
            if(state == RequestParseState.PARSE_MORE){
                return state;
            }
            else if(state != RequestParseState.HEADER_START){
                return requestError(key);
            }

            // 目前只支持处理请求 "GET"
            // 还未支持 "POST"
            state = ParseRequestUtil.parseHttpRequestHeader(context, bytes);
            if(state == RequestParseState.PARSE_MORE){
                return state;
            }
            else if(state == RequestParseState.PARSE_OK){
                context.getRequest().initRequestAttribute();
                return state;
            }
            else{
                return requestError(key);
            }


        }catch (Exception e){
            System.out.println("recvFrom: " + e);
            //TODO
        }

        return RequestParseState.PARSE_ERROR;
    }
    /**
    * @Description: 处理错误请求 关闭通道
    * @Date: 上午10:52 18-3-16
    */
    public static RequestParseState requestError(SelectionKey key){

        return RequestParseState.PARSE_ERROR;
    }

}
