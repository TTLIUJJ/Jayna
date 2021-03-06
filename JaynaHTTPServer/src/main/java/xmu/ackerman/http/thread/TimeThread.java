package xmu.ackerman.http.thread;

import xmu.ackerman.http.service.MonitorService;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 上午9:31 18-3-24
 */
public class TimeThread implements Runnable{

    private MonitorService monitorService;

    public TimeThread(MonitorService monitorService){
        this.monitorService = monitorService;
    }

    public void run(){
        try {
            monitorService.removeTask();
        }catch (Exception e){
            System.out.println("TimeThread: " + e);
        }
    }
}