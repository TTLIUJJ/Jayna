package xmu.ackerman;

import xmu.ackerman.http.context.Context;
import xmu.ackerman.http.context.HttpContext;
import xmu.ackerman.http.service.MonitorService;
import xmu.ackerman.http.thread.ReadThread;
import xmu.ackerman.http.thread.WriteThread;
import xmu.ackerman.http.service.RejectedStrategy;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午3:33 18-3-15
 */
public class JaynaHttpController {

    private final static String CONFIG_FILE =  "config.properties";
    private int port;
    private Selector selector;
    private ExecutorService threadPoolExecutor;

    //用于测试 使用keepAlive性能下降多少
    public static boolean keepAlive;
    public static long timeout;
    public static MonitorService monitorService;

    public JaynaHttpController(){ }

    /**
    * @Description: 从资源文件获取参数, 方便调试的时候使用
    * @Date: 下午4:58 18-3-21
    */
    private void initProperties(){
        try{
            File file = new File(CONFIG_FILE);
            InputStream inputStream = new FileInputStream(file);
            Properties properties = new Properties();
            properties.load(inputStream);

            Map<String, Integer> propertyMap = new HashMap<String, Integer>();

            //打印参数列表
            for(Object o : properties.keySet()){
                String key = (String) o;
                int value = Integer.parseInt(properties.getProperty(key));
                propertyMap.put(key, value);
                System.out.println( key + ": " + value);
            }

            //配置端口号
            this.port = propertyMap.get("port");

            //配置是否支持keep-alive
            int keepAliveMode = propertyMap.get("keep-alive");
            switch (keepAliveMode){
                case 0 :
                    this.keepAlive = false;
                    break;

                case 1:
                    this.keepAlive = true;
                    break;

                default:
                    throw new IllegalArgumentException("读取参数配置错误");
            }

            //timeout时间
            this.timeout = (long)propertyMap.get("timeout");

            //配置线程池参数
            int corePoolSize = propertyMap.get("corePoolSize");
            int maximumPoolSize = propertyMap.get("maximumPoolSize");
            int keepAliveTime = propertyMap.get("keepAliveTime");
            int queueSize = propertyMap.get("queueSize");
            int workQueueMode = propertyMap.get("workQueueMode");

            switch (workQueueMode){
                case 0:
                    LinkedBlockingQueue<Runnable> infiniteQueue = new LinkedBlockingQueue<Runnable>();

                    this.threadPoolExecutor = new ThreadPoolExecutor(
                            corePoolSize,
                            maximumPoolSize,
                            (long) keepAliveTime,
                            TimeUnit.SECONDS,
                            infiniteQueue,
                            new RejectedStrategy());
                    break;

                case 1:
                    ArrayBlockingQueue<Runnable> finiteQueue = new ArrayBlockingQueue<Runnable>(queueSize);

                    this.threadPoolExecutor = new ThreadPoolExecutor(
                            corePoolSize,
                            maximumPoolSize,
                            (long)keepAliveTime,
                            TimeUnit.SECONDS,
                            finiteQueue,
                            new RejectedStrategy());
                    break;


                default:
                    throw new IllegalArgumentException("读取参数配置错误");
            }

            //初始化定时任务服务类
            monitorService = new MonitorService(timeout);
        }catch (Exception e){
            System.out.println("Exception: " + e);
        }
    }

    //初始化服务器通道, 并注册selector
    private void initServerSocket(){
        ServerSocketChannel serverSocketChannel;
        Selector selector;
        try{
            serverSocketChannel = ServerSocketChannel.open();
            ServerSocket serverSocket = serverSocketChannel.socket();
            InetSocketAddress address = new InetSocketAddress(this.port);
            serverSocket.bind(address);

            //将IO请求设置为非阻塞
            serverSocketChannel.configureBlocking(false);

            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            this.selector = selector;
        }catch (Exception e){
            System.out.println("initServerSocket: " + e);
        }
    }


    public void start(){
        try {
            initProperties();
            initServerSocket();

            System.out.println("start JaynaHTTPServer");
            while (true) {
                int readyChannels = 0;
                try {
                    readyChannels = selector.select();
                } catch (Exception e) {
                    System.out.println("selector.select(): " + e);
                }

                if (readyChannels == 0) {
                    continue;
                }
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = readyKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    try {
                        if (key.isValid() && key.isAcceptable()) {
                            ServerSocketChannel server = (ServerSocketChannel) key.channel();
                            SocketChannel client = server.accept();
                            if (client != null) {
                                client.configureBlocking(false);

                                SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
                                HttpContext httpContext = new HttpContext();
                                httpContext.setContext(selector, clientKey);
                                clientKey.attach(httpContext);

                                if(keepAlive) {
                                    monitorService.addFutureTask(clientKey);
                                }
                            }
                        } else if (key.isValid() && key.isReadable()) {
                            //防止多个线程 处理一个READ_KEY
                            key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
                            Context context = (Context) key.attachment();
                            ReadThread readThread = new ReadThread(context);
                            Thread thread = new Thread(readThread);
                            threadPoolExecutor.execute(thread);

                        } else if (key.isValid() && key.isWritable()) {
                            key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
                            Context context = (Context) key.attachment();
                            WriteThread writeThread = new WriteThread(context);
                            Thread thread = new Thread(writeThread);
                            threadPoolExecutor.execute(thread);
                        }
                    }catch (Exception e){
                        System.out.println("iterator.next in loop: " + e);
                    }

                }

            }
        }catch (Exception e){
            System.out.println("start(): "+e);
        }finally {
            threadPoolExecutor.shutdownNow();
        }
    }

    public static void main(String []args){
        try {
            JaynaHttpController controller = new JaynaHttpController();
            controller.start();
        }catch (Exception e) {
            System.out.println("main: " + e);
        }
    }
}
