package xmu.ackerman.handler.entityHandler;

import xmu.ackerman.context.Context;
import xmu.ackerman.handler.HtmlHandler;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午7:16 18-3-16
 */
public class News1Handler extends HtmlHandler{
    private final String fileName = "/news/news1.html";

    @Override
    public String getFilename(){ return fileName; }

    @Override
    public void doGet(Context context){

    }
}
