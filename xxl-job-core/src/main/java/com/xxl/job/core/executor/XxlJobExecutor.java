package com.xxl.job.core.executor;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.client.AdminBizClient;
import com.xxl.job.core.biz.impl.ExecutorBizImpl;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.thread.ExecutorRegistryThread;
import com.xxl.job.core.thread.JobLogFileCleanThread;
import com.xxl.job.core.thread.JobThread;
import com.xxl.job.core.thread.TriggerCallbackThread;
import com.xxl.rpc.registry.ServiceRegistry;
import com.xxl.rpc.remoting.net.impl.netty_http.server.NettyHttpServer;
import com.xxl.rpc.remoting.provider.XxlRpcProviderFactory;
import com.xxl.rpc.serialize.Serializer;
import com.xxl.rpc.serialize.impl.HessianSerializer;
import com.xxl.rpc.util.IpUtil;
import com.xxl.rpc.util.NetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by xuxueli on 2016/3/2 21:14.
 */
public class XxlJobExecutor  {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobExecutor.class);

    // ---------------------- param ----------------------
    private String adminAddresses;
    private String appName;
    private String ip;
    private int port;
    private String accessToken;
    private String logPath;
    private int logRetentionDays;

    public void setAdminAddresses(String adminAddresses) {
        this.adminAddresses = adminAddresses;
    }
    public void setAppName(String appName) {
        this.appName = appName;
    }
    public void setIp(String ip) {
        this.ip = ip;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }
    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }


    // ---------------------- start + stop ----------------------
    public void start() throws Exception {

        // init logpath
        XxlJobFileAppender.initLogPath(logPath);

        // init invoker, admin-client
        initAdminBizList(adminAddresses, accessToken);


        // init JobLogFileCleanThread
        JobLogFileCleanThread.getInstance().start(logRetentionDays);

        // init TriggerCallbackThread
        TriggerCallbackThread.getInstance().start();
    }
    public void destroy(){
        // destory executor-server
        stopRpcProvider();

        // destory jobThreadRepository
        if (jobThreadRepository.size() > 0) {
            for (Integer key : new HashSet<>(jobThreadRepository.keySet())) {
                removeJobThread(key, "web container destroy and kill the job.", true, true);
            }
            jobThreadRepository.clear();
        }
        jobHandlerRepository.clear();

        // destory JobLogFileCleanThread
        JobLogFileCleanThread.getInstance().toStop();

        // destory TriggerCallbackThread
        TriggerCallbackThread.getInstance().toStop();

    }


    // ---------------------- admin-client (rpc invoker) ----------------------
    private static List<AdminBiz> adminBizList;
    private static Serializer serializer = new HessianSerializer();
    private void initAdminBizList(String adminAddresses, String accessToken) throws Exception {
        if (adminAddresses!=null && adminAddresses.trim().length()>0) {
            for (String address: adminAddresses.trim().split(",")) {
                if (address!=null && address.trim().length()>0) {

                    AdminBiz adminBiz = new AdminBizClient(address.trim(), accessToken);

                    if (adminBizList == null) {
                        adminBizList = new ArrayList<AdminBiz>();
                    }
                    adminBizList.add(adminBiz);
                }
            }
        }
    }
    public static List<AdminBiz> getAdminBizList(){
        return adminBizList;
    }
    public static Serializer getSerializer() {
        return serializer;
    }


    // ---------------------- executor-server (rpc provider) ----------------------
    private XxlRpcProviderFactory xxlRpcProviderFactory = null;

    private void initRpcProvider(String ip, int port, String appName, String accessToken) throws Exception {

        if (xxlRpcProviderFactory != null) {
            return;
        }

        // init, provider factory
        String address = IpUtil.getIpPort(ip, port);
        Map<String, String> serviceRegistryParam = new HashMap<String, String>();
        serviceRegistryParam.put("appName", appName);
        serviceRegistryParam.put("address", address);

        xxlRpcProviderFactory = new XxlRpcProviderFactory();

        xxlRpcProviderFactory.setServer(NettyHttpServer.class);
        xxlRpcProviderFactory.setSerializer(HessianSerializer.class);
        xxlRpcProviderFactory.setCorePoolSize(20);
        xxlRpcProviderFactory.setMaxPoolSize(200);
        xxlRpcProviderFactory.setIp(ip);
        xxlRpcProviderFactory.setPort(port);
        xxlRpcProviderFactory.setAccessToken(accessToken);
        xxlRpcProviderFactory.setServiceRegistry(ExecutorServiceRegistry.class);
        xxlRpcProviderFactory.setServiceRegistryParam(serviceRegistryParam);

        // add services
        xxlRpcProviderFactory.addService(ExecutorBiz.class.getName(), null, new ExecutorBizImpl());

        // start
        xxlRpcProviderFactory.start();

    }

    public static class ExecutorServiceRegistry extends ServiceRegistry {

        @Override
        public void start(Map<String, String> param) {
            // start registry
            ExecutorRegistryThread.getInstance().start(param.get("appName"), param.get("address"));
        }
        @Override
        public void stop() {
            // stop registry
            ExecutorRegistryThread.getInstance().toStop();
        }

        @Override
        public boolean registry(Set<String> keys, String value) {
            return false;
        }
        @Override
        public boolean remove(Set<String> keys, String value) {
            return false;
        }
        @Override
        public Map<String, TreeSet<String>> discovery(Set<String> keys) {
            return null;
        }
        @Override
        public TreeSet<String> discovery(String key) {
            return null;
        }

    }

    private void startRpcProvider() {
        try {
            port = port > 0 ? port : NetUtil.findAvailablePort(9999);
            ip = (ip != null && ip.trim().length() > 0) ? ip : IpUtil.getIp();
            initRpcProvider(ip, port, appName, accessToken);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void stopRpcProvider() {
        // stop provider factory
        try {
            if (xxlRpcProviderFactory != null) {
                xxlRpcProviderFactory.stop();
                xxlRpcProviderFactory = null;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void stopRpcProviderRegistry() {
        ExecutorRegistryThread.getInstance().toStop();
    }

    public void breakJob() {
        // In order to execute unregistered executor job, so don't stop rpc provider
        //stopRpcProvider();
        stopRpcProviderRegistry();
        breakAllJobs();
    }

    public void continueJob() {
        continueAllJobs();
        stopRpcProvider();
        startRpcProvider();
    }

    public void continueJobNotRegistry() {
        // start rpc provider but not registry
        stopRpcProviderRegistry();
        startRpcProvider();
    }

    private void breakAllJobs() {
        for (JobThread jobThread : new ArrayList<>(jobThreadRepository.values())) {
            jobThread.breakJob();
        }
    }

    private void continueAllJobs() {
        for (JobThread jobThread : new ArrayList<>(jobThreadRepository.values())) {
            jobThread.continueJob();
        }
    }

    // ---------------------- job handler repository ----------------------
    private static ConcurrentMap<String, IJobHandler> jobHandlerRepository = new ConcurrentHashMap<String, IJobHandler>();
    public static IJobHandler registJobHandler(String name, IJobHandler jobHandler){
        logger.info(">>>>>>>>>>> xxl-job register jobhandler success, name:{}, jobHandler:{}", name, jobHandler);
        return jobHandlerRepository.put(name, jobHandler);
    }
    public static IJobHandler loadJobHandler(String name){
        return jobHandlerRepository.get(name);
    }


    // ---------------------- job thread repository ----------------------
    private static ConcurrentMap<Integer, JobThread> jobThreadRepository = new ConcurrentHashMap<Integer, JobThread>();
    private static String jobThreadLock = new String();

    public static String getJobThreadLock() {
        return jobThreadLock;
    }

    public static JobThread registJobThread(int jobId, IJobHandler handler, String removeOldReason){
        JobThread newJobThread = new JobThread(jobId, handler);
        newJobThread.start();
        logger.info(">>>>>>>>>>> xxl-job regist JobThread success, jobId:{}, handler:{}", new Object[]{jobId, handler});

        JobThread oldJobThread = null;
        synchronized (jobThreadLock) {
            oldJobThread = jobThreadRepository.put(jobId, newJobThread);
        }

        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            //这个地方不能等待，不然有死锁隐患
        }

        return newJobThread;
    }
    public static void removeJobThread(int jobId, String removeOldReason, boolean wait, boolean force){
        JobThread oldJobThread = null;
        synchronized (jobThreadLock) {
            oldJobThread = jobThreadRepository.get(jobId);
            if (oldJobThread == null) {
                return;
            }
            if (force == false && oldJobThread.isIdleTimesOver() == false) {
                return;
            }
            jobThreadRepository.remove(jobId);
        }

        oldJobThread.toStop(removeOldReason);
        if (wait) {
            try {
                oldJobThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
    public static JobThread loadJobThread(int jobId){
        synchronized (jobThreadLock) {
            JobThread jobThread = jobThreadRepository.get(jobId);
            if (jobThread != null) {
                jobThread.resetIdleTimes();
            }
            return jobThread;
        }
    }

    public static boolean isJobMustExecute() {
        return JobThread.isJobMustExecute();
    }

    //暂时屏蔽，不供外界使用，请使用JobExecutor.isBreadJob()
    private static boolean isBreakJob() {
        JobThread jobThread = JobThread.getCurrentJobThread();
        if (jobThread != null) {
            return jobThread.isStop() || jobThread.isBreaking();
        }
        return true;
    }

}
