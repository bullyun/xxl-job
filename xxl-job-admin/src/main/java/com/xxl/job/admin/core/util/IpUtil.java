package com.xxl.job.admin.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: yangrusheng
 * @Description:
 * @Date: Created in 17:14 2020/8/4
 * @Modified By:
 */
public class IpUtil {

    public static boolean isIp(String ipAddress) {
        if (ipAddress == null) {
            return false;
        }
        String ip = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pattern = Pattern.compile(ip);
        Matcher matcher = pattern.matcher(ipAddress);
        return matcher.matches();
    }

}
