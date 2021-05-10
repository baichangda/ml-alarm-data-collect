package com.bcd.config.hbase;

import com.bcd.base.exception.BaseRuntimeException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


@Configuration
public class HBaseUtil {

    public static final String TELEMETRY_JSON = "saic:json";
    public static final String ALARM_TABLE = "saic:alarm";
    private static final String C_ZERO32 = "00000000000000000000000000000000";
    private static final String C_SHARP32 = "################################";
    /**
     * json表拆分日期，单位秒
     */
    public static Long jsonTime = 0L;
    private static Logger logger = LoggerFactory.getLogger(HBaseUtil.class);
    private static org.apache.hadoop.conf.Configuration configuration = null;
    private static Connection connection = null;
    private static ScheduledExecutorService connPool = Executors.newScheduledThreadPool(1);
    private static ScheduledExecutorService workPool = Executors.newScheduledThreadPool(1);
    private static DateTimeFormatter dtf1 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("+8"));
    private static DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("+8"));
    private static String json = "saic:json_";

    public HBaseUtil(@Value("${hbase.zookeeper.quorum}") String zookeeper,
                     @Value("${hbase.zookeeper.property.clientPort}") String port) {
        try {
            configuration = HBaseConfiguration.create();
            configuration.set("hbase.zookeeper.quorum", zookeeper);
            configuration.set("hbase.zookeeper.property.clientPort", port);
            connection = ConnectionFactory.createConnection(configuration, connPool);
        } catch (IOException e) {
            throw BaseRuntimeException.getException(e);
        }
    }

    /**
     * 获取table
     *
     * @param tableName 表名
     * @return Table
     * @throws IOException IOException
     */
    private static Table getTable(String tableName) throws IOException {
        return connection.getTable(TableName.valueOf(tableName), workPool);
    }

    public static List<Map<String,String>> queryAlarms() {
        Scan scan = new Scan();
        return queryDataMap(ALARM_TABLE, scan);
    }

    public static List<String> querySignals(String vin, Date startTime, Date endTime) {
        //根据时间获取表名
        List<String> list = new ArrayList<>();
        Long tboxTime = startTime.getTime() / 1000;
        if (tboxTime.equals(jsonTime)) {
            String table = TELEMETRY_JSON;
            List<String> list1 = queryJsonData(vin, startTime, endTime, table);
            list.addAll(list1);

            table = makeJsonTable(tboxTime);
            List<String> list2 = queryJsonData(vin, startTime, endTime, table);
            list.addAll(list2);
        } else {
            String table = TELEMETRY_JSON;
            if (tboxTime >= jsonTime) {
                table = makeJsonTable(tboxTime);
            }
            List<String> list1 = queryJsonData(vin, startTime, endTime, table);
            list.addAll(list1);
        }
        return list;
    }

    private static List<String> queryJsonData(String vin, Date startTime, Date endTime, String tableName) {
        // 参数不能缺少
        if (null == vin || null == startTime || null == endTime) {
            throw new IllegalArgumentException("the params can't be null");
        }
        // 查询开始时间必须小于结束时间
        if (startTime.getTime() > endTime.getTime()) {
            throw new IllegalArgumentException("the end time must be bigger than the start time");
        }

        String startRowKey = makeDataObjectRowKey(vin, dtf1.format(startTime.toInstant()));
        String endRowKey = makeDataObjectRowKey(vin, dtf1.format(endTime.toInstant()));

        Scan scan = new Scan();

        if (StringUtils.isNoneBlank(startRowKey) && StringUtils.isNoneBlank(endRowKey)) {
            scan.setStartRow(Bytes.toBytes(startRowKey));
            scan.setStopRow(Bytes.toBytes(endRowKey));
        }

        return queryDataString(tableName, scan);
    }

    private static List<String> queryDataString(String tableName, Scan scan) {
        ResultScanner rs = null;
        // 获取表
        Table table = null;
        List<String> dataList = new ArrayList<>();
        try {
            table = getTable(tableName);
            rs = table.getScanner(scan);

            for (Result r : rs) {
                Map<String,String> data=new HashMap<>();
                for (Cell cell : r.listCells()) {
                    String jsonString = Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());
                    if (StringUtils.isNotBlank(jsonString)) {
                        try {
                            dataList.add(jsonString);
                        } catch (Exception e) {
                            e.printStackTrace();
                            logger.error("数据转换异常:{0}", jsonString);
                        }
                    }
                }

            }
        } catch (IOException e) {
            logger.error(MessageFormat.format("遍历查询指定表中的所有数据失败,tableName:{0}"
                    , tableName), e);
            e.printStackTrace();
        } finally {
            close(null, rs, table);
        }

        return dataList;
    }

    /**
     * 00
     * 通过表名以及过滤条件查询数据
     *
     * @param tableName 表名
     * @param scan      过滤条件
     * @return List<T>
     * @author sunjun
     * @date 2018/10/23 10:13
     * @since 1.0.0
     */
    private static List<Map<String,String>> queryDataMap(String tableName, Scan scan) {
        ResultScanner rs = null;
        // 获取表
        Table table = null;
        List<Map<String,String>> dataList = new ArrayList<>();
        try {
            table = getTable(tableName);
            rs = table.getScanner(scan);

            for (Result r : rs) {
                Map<String,String> data=new HashMap<>();
                for (Cell cell : r.listCells()) {
                    String column = Bytes.toString(cell.getQualifierArray(), cell.getQualifierOffset(), cell.getQualifierLength());
                    String jsonString = Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());
                    if (StringUtils.isNotBlank(jsonString)) {
                        try {
                            data.put(column,jsonString);
                        } catch (Exception e) {
                            e.printStackTrace();
                            logger.error("数据转换异常:{0}", jsonString);
                        }
                    }
                }
                dataList.add(data);
            }
        } catch (IOException e) {
            logger.error(MessageFormat.format("遍历查询指定表中的所有数据失败,tableName:{0}"
                    , tableName), e);
            e.printStackTrace();
        } finally {
            close(null, rs, table);
        }

        return dataList;
    }

    /**
     * 关闭流
     */
    private static void close(Admin admin, ResultScanner rs, Table table) {
        if (admin != null) {
            try {
                admin.close();
            } catch (IOException e) {
                logger.error("关闭Admin失败", e);
            }
        }

        if (rs != null) {
            rs.close();
        }

        if (table != null) {
            try {
                table.close();
            } catch (IOException e) {
                logger.error("关闭Table失败", e);
            }
        }
    }

    /**
     * 产生行程数据的rowkey（方便查询）
     *
     * @param vin      车辆vin码
     * @param dataTime 采集时间
     * @return
     */
    private static String makeDataObjectRowKey(String vin, String dataTime) {
        return String.format("%s%s%s", calcMd5(vin).substring(0, 4),
                prependForceLen(vin, 20), appendForceLen(dataTime, 18));
    }

    /**
     * 获取MD5编码
     *
     * @param value
     * @return
     */
    private static String calcMd5(String value) {

        try {
            MessageDigest s_md5 = MessageDigest.getInstance("MD5");

            byte[] md5 = s_md5.digest(value.getBytes("utf-8"));
            return Hex.encodeHexString(md5);
        } catch (NoSuchAlgorithmException ex) {
            // ignore
        } catch (UnsupportedEncodingException ex) {
            // ignore
        }

        return null;
    }


    /**
     * 前面添0方式强制字符串长度,用于将整形ID补充为等长字符串且不影响按大小排序
     *
     * @param value
     * @param len
     * @return
     */
    private static String prependForceLen(String value, int len) {

        if (value == null)
            value = "";

        if (value.length() == len)
            return value;
        else if (value.length() > len)
            return value.substring(0, len);
        else {
            // 为了提高性能,不太长的字符串不使用StringBuilder来补足
            int lenPad = len - value.length();
            if (lenPad <= C_ZERO32.length()) {
                return C_ZERO32.substring(0, lenPad) + value;
            } else {
                int sbXlen = len - value.length();
                StringBuilder sbX = new StringBuilder(sbXlen);
                sbX.setLength(sbXlen);

                for (int i = 0; i < sbXlen; i++)
                    sbX.setCharAt(i, '0');
                sbX.append(value);
                return sbX.toString();
            }
        }
    }

    /**
     * 追加#方式强制字符串长度
     *
     * @param value
     * @param len
     * @return
     */
    private static String appendForceLen(String value, int len) {

        if (value == null)
            value = "";

        if (value.length() == len)
            return value;
        else if (value.length() > len)
            return value.substring(0, len);
        else {
            // 为了提高性能,不太长的字符串不使用StringBuilder来补足
            int lenPad = len - value.length();
            if (lenPad <= C_SHARP32.length()) {
                return value + C_SHARP32.substring(0, lenPad);
            } else {
                StringBuilder sbX = new StringBuilder(len);
                sbX.append(value);
                sbX.setLength(len);
                for (int i = value.length(); i < len; i++)
                    sbX.setCharAt(i, '#');
                return sbX.toString();
            }
        }
    }


    /**
     * 根据采集时间生成json表名
     *
     * @param tboxTime
     * @return
     */
    public static String makeJsonTable(Long tboxTime) {
        Date date = new Date(tboxTime * 1000);
        String dateStr = dtf2.format(date.toInstant());
        String[] strs = dateStr.split("-");
        String month = strs[1];

        return json + month + "m";
    }

}
