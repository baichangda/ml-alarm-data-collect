package com.bcd.ml.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.bcd.base.exception.BaseRuntimeException;
import com.bcd.base.util.DateZoneUtil;
import com.bcd.base.util.JsonUtil;
import com.bcd.base.support_hbase.HBaseUtil;
import com.bcd.parser.impl.gb32960.Parser_gb32960;
import com.bcd.parser.impl.gb32960.data.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MlService {

    Logger logger = LoggerFactory.getLogger(MlService.class);

    ZoneOffset zoneOffset = ZoneOffset.of("+8");

    @Value("${workPoolSize}")
    int workPoolSize;

    @Value("${alarmTimeOffset}")
    int alarmTimeOffset;

    @Value("${alarmSourcePath}")
    String alarmSourcePath;

    @Value("${signalSourcePath}")
    String signalSourcePath;

    @Value("${gb_signalSourcePath}")
    String gb_signalSourcePath;

    @Value("${vehicleInfoPath}")
    String vehicleInfoPath;

    @Value("${vehicleInfoPwd}")
    String vehicleInfoPwd;

    @Autowired
    MongoTemplate mongoTemplate;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("+8"));

    Parser_gb32960 parser_gb32960 = new Parser_gb32960(false);

    {
        parser_gb32960.init();
    }


    public int[] saveToMongo(String alarmCollection, String signalCollection) {
        ScheduledExecutorService monitorPool = Executors.newScheduledThreadPool(1);
        ExecutorService alarmPool = Executors.newSingleThreadExecutor();
        AtomicInteger allAlarmCount = new AtomicInteger();
        AtomicInteger allSignalCount = new AtomicInteger();
        AtomicInteger alarmCount = new AtomicInteger();
        AtomicInteger signalCount = new AtomicInteger();
        int period = 5;
        monitorPool.scheduleWithFixedDelay(() -> {
            int count1 = alarmCount.getAndSet(0);
            int count2 = signalCount.getAndSet(0);
            logger.info("count[{},{}] speed[{},{}]", allAlarmCount.get(), allSignalCount.get(), count1 / period, count2 / period);
        }, period, period, TimeUnit.SECONDS);

        int batch = 1000;
        alarmPool.execute(() -> {
            mongoTemplate.remove(new Query(), alarmCollection);
            try (BufferedReader br = Files.newBufferedReader(Paths.get(alarmSourcePath))) {
                List<String> tempList = new ArrayList<>();
                String line;
                while ((line = br.readLine()) != null) {
                    tempList.add(line);
                    if (tempList.size() == batch) {
                        mongoTemplate.insert(tempList, alarmCollection);
                        alarmCount.addAndGet(batch);
                        allAlarmCount.addAndGet(batch);
                        tempList.clear();
                    }
                }
                mongoTemplate.insert(tempList, "alarm_all");
                alarmCount.addAndGet(tempList.size());
                allAlarmCount.addAndGet(tempList.size());
            } catch (IOException e) {
                throw BaseRuntimeException.getException(e);
            }
        });

        if (signalCollection != null) {
            mongoTemplate.remove(new Query(), "signal_all");
            try (BufferedReader br = Files.newBufferedReader(Paths.get(signalSourcePath))) {
                List<String> tempList = new ArrayList<>();
                String line;
                while ((line = br.readLine()) != null) {
                    tempList.add(line);
                    if (tempList.size() == batch) {
                        mongoTemplate.insert(tempList, "signal_all");
                        signalCount.addAndGet(batch);
                        allSignalCount.addAndGet(batch);
                        tempList.clear();
                    }
                }
                mongoTemplate.insert(tempList, "signal_all");
                signalCount.addAndGet(tempList.size());
                allSignalCount.addAndGet(tempList.size());
            } catch (IOException e) {
                throw BaseRuntimeException.getException(e);
            }
        }

        try {
            alarmPool.shutdown();
            while (!alarmPool.awaitTermination(60, TimeUnit.SECONDS)) {

            }
            monitorPool.shutdown();
            while (!monitorPool.awaitTermination(60, TimeUnit.SECONDS)) {

            }
        } catch (InterruptedException e) {
            throw BaseRuntimeException.getException(e);
        }

        logger.info("finish save mongo alarm[{}] signal[{}]", allAlarmCount.get(), allSignalCount.get());
        return new int[]{allAlarmCount.get(), allSignalCount.get()};

    }

    /**
     * @param alarmStartTimeStr
     * @param alarmEndTimeStr
     * @return
     */
    public int[] fetchAndSave(String alarmStartTimeStr, String alarmEndTimeStr) {
        long alarmStartTime = DateZoneUtil.stringToDate_day(alarmStartTimeStr).getTime();
        long alarmEndTime = DateZoneUtil.stringToDate_day(alarmEndTimeStr).getTime();
        List<Map<String, String>> alarmList = HBaseUtil.queryAlarms(e -> {
            String alarmLevel = e.get("alarmLevel");
            String platformCode = e.get("platformCode");
            if ("3".equals(alarmLevel) && ("gb".equals(platformCode) || "gb-private".equals(platformCode))) {
                long cur = DateZoneUtil.stringToDate_second(e.get("beginTime")).getTime();
                return cur >= alarmStartTime && cur <= alarmEndTime;
            } else {
                return false;
            }
        });
        int size = alarmList.size();
        logger.info("fetch filter alarm[{}]", size);
        if (size == 0) {
            return new int[]{0, 0};
        }

        AtomicInteger processedAlarmCount = new AtomicInteger();
        AtomicInteger processedSignalCount = new AtomicInteger();
        AtomicInteger saveAlarmCount = new AtomicInteger();
        AtomicInteger saveSignalCount = new AtomicInteger();
        ScheduledExecutorService monitorPool = Executors.newScheduledThreadPool(1);
        int period = 5;
        monitorPool.scheduleWithFixedDelay(() -> {
            logger.info("allAlarm[{}] processedAlarm[{}] processedSignal[{}] saveAlarm[{}] saveSignal[{}]"
                    , size,processedAlarmCount.get(),processedSignalCount.get(),saveAlarmCount.get(),saveSignalCount.get());
        }, period, period, TimeUnit.SECONDS);


        AtomicBoolean stop = new AtomicBoolean(false);


        ArrayBlockingQueue<Map<String, String>> alarmQueue = new ArrayBlockingQueue<>(5000);
        ArrayBlockingQueue<String> signalQueue = new ArrayBlockingQueue<>(5000);
        ExecutorService alarmPool = Executors.newSingleThreadExecutor();
        ExecutorService signalPool = Executors.newSingleThreadExecutor();
        alarmPool.execute(() -> {
            String alarmFilePath = "alarm.txt";
            try {
                Files.deleteIfExists(Paths.get(alarmFilePath));
                Files.createFile(Paths.get(alarmFilePath));
            } catch (IOException e) {
                throw BaseRuntimeException.getException(e);
            }
            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(alarmFilePath))) {
                while (!stop.get()) {
                    Map<String, String> data = alarmQueue.poll(3, TimeUnit.SECONDS);
                    if (data != null) {
                        bw.write(JsonUtil.toJson(data));
                        bw.newLine();
                        bw.flush();
                        saveAlarmCount.incrementAndGet();
                    }
                }

            } catch (IOException | InterruptedException e) {
                throw BaseRuntimeException.getException(e);
            }
        });

        signalPool.execute(() -> {
            String signalsFilePath = "signal.txt";
            try {
                Files.deleteIfExists(Paths.get(signalsFilePath));
                Files.createFile(Paths.get(signalsFilePath));
            } catch (IOException e) {
                throw BaseRuntimeException.getException(e);
            }
            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(signalsFilePath))) {
                while (!stop.get()) {
                    String signalJsonStr = signalQueue.poll(3, TimeUnit.SECONDS);
                    if (signalJsonStr != null) {
                        bw.write(signalJsonStr);
                        bw.newLine();
                        bw.flush();
                        saveSignalCount.addAndGet(30);
                    }
                }

            } catch (IOException | InterruptedException e) {
                throw BaseRuntimeException.getException(e);
            }
        });


        //检测重复的alarm
        Set<String> set1 = new HashSet<>();
        //检测相同vin和time、但是不同type的 alarm
        Map<String, Integer> map2 = new ConcurrentHashMap<>();
        //检测重复的signal
        Set<String> set3 = ConcurrentHashMap.newKeySet();

        ArrayBlockingQueue<Map<String, String>> workQueue = new ArrayBlockingQueue<>(5000);
        ExecutorService[] workPools = new ExecutorService[workPoolSize];
        for (int i = 0; i < workPools.length; i++) {
            workPools[i] = Executors.newSingleThreadExecutor();
            workPools[i].execute(() -> {
                try {
                    while (!stop.get()) {
                        Map<String, String> alarm = workQueue.poll(3, TimeUnit.SECONDS);
                        if (alarm != null) {
                            String vin = alarm.get("vin");
                            String beginTime = alarm.get("beginTime");
                            String alarmType = alarm.get("alarmType");
                            String platformCode = alarm.get("platformCode");
                            Date alarmTime = DateZoneUtil.stringToDate_second(beginTime);
                            LocalDateTime ldt = LocalDateTime.ofInstant(alarmTime.toInstant(), zoneOffset);
                            Date d1 = Date.from(ldt.plusSeconds(-alarmTimeOffset).toInstant(zoneOffset));
                            Date d2 = Date.from(ldt.plusSeconds(alarmTimeOffset).toInstant(zoneOffset));
                            List<String[]> signals = HBaseUtil.querySignals(vin, d1, d2);
                            int signalSize = signals.size();
                            String key1 = vin + "-" + beginTime + "-" + alarmType + "-" + platformCode;
                            String key2 = vin + "-" + beginTime;
                            Integer old = map2.putIfAbsent(key2, signalSize);
                            if (old == null) {
                                if (signalSize > 0) {
                                    processedAlarmCount.incrementAndGet();
                                    alarmQueue.put(alarm);
                                    for (String[] arr : signals) {
                                        String signalTime = arr[0].substring(29, 43);
                                        String signalJson = arr[1];
                                        String key3 = vin + "-" + signalTime;
                                        if (!set3.contains(key3)) {
                                            synchronized (key3.intern()) {
                                                if (set3.contains(key3)) {
                                                    continue;
                                                } else {
                                                    set3.add(key3);
                                                }
                                            }
                                            processedSignalCount.addAndGet(30);
                                            signalQueue.put(signalJson);
                                        }
                                    }
                                } else {
                                    logger.info("no signal alarm[{}}]", key1);
                                }
                            } else {
                                if (old > 0) {
                                    processedAlarmCount.incrementAndGet();
                                    alarmQueue.put(alarm);
                                } else {
                                    logger.info("no signal alarm[{}] in map2", key1);
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    throw BaseRuntimeException.getException(e);
                }
            });
        }


        //split alarm
        Iterator<Map<String, String>> it = alarmList.iterator();
        while (it.hasNext()) {
            try {
                Map<String, String> alarm = it.next();
                String vin = alarm.get("vin");
                String beginTime = alarm.get("beginTime");
                String alarmType = alarm.get("alarmType");
                String platformCode = alarm.get("platformCode");
                String key1 = vin + "-" + beginTime + "-" + alarmType + "-" + platformCode;
                if (!set1.contains(key1)) {
                    set1.add(key1);
                    String key2 = vin + "-" + beginTime;
                    Integer val2 = map2.get(key2);
                    if (val2 == null) {
                        workQueue.put(alarm);
                    } else {
                        if (val2 > 0) {
                            processedAlarmCount.incrementAndGet();
                            alarmQueue.put(alarm);
                        } else {
                            logger.info("no signal alarm[{}] in map2", key1);
                        }
                    }
                } else {
                    logger.info("duplicate alarm[{}]", key1);
                }
            } catch (InterruptedException e) {
                logger.error("parse alarm error", e);
            }
            it.remove();
        }


        try {
            while (!workQueue.isEmpty()) {
                TimeUnit.SECONDS.sleep(1);
            }
            while (!alarmQueue.isEmpty()) {
                TimeUnit.SECONDS.sleep(1);
            }
            while (!signalQueue.isEmpty()) {
                TimeUnit.SECONDS.sleep(1);
            }
            stop.set(true);
            for (ExecutorService workPool : workPools) {
                workPool.shutdown();
            }
            alarmPool.shutdown();
            signalPool.shutdown();
            for (ExecutorService workPool : workPools) {
                while (!workPool.awaitTermination(60, TimeUnit.SECONDS)) {

                }
            }
            while (!alarmPool.awaitTermination(60, TimeUnit.SECONDS)) {

            }
            while (!signalPool.awaitTermination(60, TimeUnit.SECONDS)) {

            }
            monitorPool.shutdown();
            while (!monitorPool.awaitTermination(60, TimeUnit.SECONDS)) {

            }
        } catch (InterruptedException e) {
            throw BaseRuntimeException.getException(e);
        }

        logger.info("finish alarm[{}] signal[{}]", saveAlarmCount.get(), saveSignalCount.get());

        return new int[]{saveAlarmCount.get(), saveSignalCount.get()};
    }

    public int fetchAndSave_gb(int num) {
        AtomicInteger count = new AtomicInteger();
        AtomicInteger monitorCount = new AtomicInteger();
        ScheduledExecutorService monitorPool = Executors.newSingleThreadScheduledExecutor();
        monitorPool.scheduleWithFixedDelay(() -> {
            logger.info("fetch count:{} speed:{}", count.get(), monitorCount.getAndSet(0) / 3);
        }, 3, 3, TimeUnit.SECONDS);
        Path path = Paths.get(gb_signalSourcePath);
        try (BufferedWriter bw = Files.newBufferedWriter(path)) {
            Function<Map<String, String>, Boolean> function = e -> {
                try {
                    bw.write(e.get("message"));
                    bw.newLine();
                    count.incrementAndGet();
                    monitorCount.incrementAndGet();
                } catch (IOException ex) {
                    throw BaseRuntimeException.getException(ex);
                }
                return count.get() < num;
            };
            HBaseUtil.querySignals_gb(function);
            bw.flush();
        } catch (IOException e) {
            throw BaseRuntimeException.getException(e);
        } finally {
            monitorPool.shutdown();
            try {
                while (!monitorPool.awaitTermination(60, TimeUnit.SECONDS)) {

                }
            } catch (InterruptedException ex) {
                logger.error("interrupted", ex);
            }
        }
        logger.info("finish all[{}]", count.get());
        return count.get();
    }

    public Map<String, String> initVinToVehicleType() {
        Map<String, String> vinToVehicleType = new HashMap<>();
        EasyExcel.read(vehicleInfoPath).password(vehicleInfoPwd)
                .registerReadListener(new AnalysisEventListener() {
                    @Override
                    public void invoke(Object data, AnalysisContext context) {
                        final String vin = Optional.ofNullable(((LinkedHashMap<Integer, Object>) data).get(0)).map(Object::toString).orElse(null);
                        final String vehicleType = Optional.ofNullable(((LinkedHashMap<Integer, Object>) data).get(2)).map(Object::toString).orElse(null);
                        if (vin != null && vehicleType != null) {
                            vinToVehicleType.put(vin, vehicleType);
                        }
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext context) {
                        logger.info("finish read excel count[{}]", vinToVehicleType.size());
                    }
                }).doReadAll();
        return vinToVehicleType;
    }

    public int saveToMongo_gb() {

        Map<String, String> vinToVehicleType = initVinToVehicleType();

        AtomicInteger count = new AtomicInteger();
        AtomicInteger monitorCount = new AtomicInteger();
        ScheduledExecutorService monitorPool = Executors.newSingleThreadScheduledExecutor();
        monitorPool.scheduleWithFixedDelay(() -> {
            logger.info("fetch count:{} speed:{}", count.get(), monitorCount.getAndSet(0) / 3);
        }, 3, 3, TimeUnit.SECONDS);

        mongoTemplate.remove(new Query(), "signal_gb");
        Path path = Paths.get(gb_signalSourcePath);

        AtomicInteger vinNum = new AtomicInteger(1);
        Map<String, String> vin_randomVin = new HashMap<>();

        List<Object[]> vehicleCommonDataFieldList = new ArrayList<>();
        Field[] declaredFields = VehicleCommonData.class.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            declaredField.setAccessible(true);
            Field[] innerDeclaredFields = declaredField.getType().getDeclaredFields();
            for (Field innerDeclaredField : innerDeclaredFields) {
                innerDeclaredField.setAccessible(true);
            }
            vehicleCommonDataFieldList.add(new Object[]{declaredField, innerDeclaredFields});
        }

        List<Map<String, Object>> tempList = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                byte[] bytes = ByteBufUtil.decodeHexDump(line);
                ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
                final Packet packet = parser_gb32960.parse(Packet.class, byteBuf);
                Map<String, Object> curDataMap = new HashMap<>();
                String vehicleType = vinToVehicleType.get(packet.getVin());
                if (vehicleType == null) {
                    logger.info("vin[{}] no mapped vehicleType,discard data[{}]", packet.getVin(), line);
                    continue;
                }
                //数据脱敏处理
                String vin = vin_randomVin.computeIfAbsent(packet.getVin(), e -> {
                    return "TEST" + Strings.padStart("" + vinNum.getAndIncrement(), 13, '0');
                });
                curDataMap.put("vin", vin);
                curDataMap.put("vehicleType", vehicleType);
                PacketData packetData = packet.getData();
                VehicleCommonData vehicleCommonData;
                if (packetData instanceof VehicleRealData) {
                    vehicleCommonData = ((VehicleRealData) packetData).getVehicleCommonData();
                    curDataMap.put("collectTime", ((VehicleRealData) packetData).getCollectTime());
                } else if (packetData instanceof VehicleSupplementData) {
                    vehicleCommonData = ((VehicleSupplementData) packetData).getVehicleCommonData();
                    curDataMap.put("collectTime", ((VehicleSupplementData) packetData).getCollectTime());
                } else {
                    logger.info("PacketData class is [{}],discard data[{}]", packetData.getClass().getName(), line);
                    continue;
                }
                for (Object[] objects : vehicleCommonDataFieldList) {
                    Field f1 = (Field) objects[0];
                    Object o1 = f1.get(vehicleCommonData);
                    if (o1 != null) {
                        Field[] f2_arr = (Field[]) objects[1];
                        for (Field f2 : f2_arr) {
                            Object o2 = f2.get(o1);
                            curDataMap.put(f1.getName() + "_" + f2.getName(), o2);
                        }
                    }
                }

                tempList.add(curDataMap);
                count.incrementAndGet();
                monitorCount.incrementAndGet();
                if (tempList.size() == 10000) {
                    mongoTemplate.insert(tempList, "signal_gb");
                    tempList.clear();
                }
            }
            if (tempList.size() > 0) {
                mongoTemplate.insert(tempList, "signal_gb");
            }
        } catch (IOException | IllegalAccessException e) {
            throw BaseRuntimeException.getException(e);
        } finally {
            monitorPool.shutdown();
            try {
                while (!monitorPool.awaitTermination(60, TimeUnit.SECONDS)) {

                }
            } catch (InterruptedException ex) {
                logger.error("interrupted", ex);
            }
        }
        return count.get();
    }

    public void parseAlarmTxt(MultipartFile alarmTxtFile, HttpServletResponse response) {
        try {
            String[] header = new String[]{"key", "vin", "workModel", "vehicleType", "alarm_level",
                    "alarm_name", "alarmType", "beginTime", "end_time", "mileage", "platform_code",
                    "platform_name", "saleStatus", "issue", "handleruser", "handlertime", "handlerstatus", "is_alarm"};
            final BufferedReader br = new BufferedReader(new InputStreamReader(alarmTxtFile.getInputStream()));
            List<List> lineList = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                String[] arr = line.split("\t");
                lineList.add(new ArrayList<>(Arrays.asList(arr)));
            }
            for (int i = 0; i < lineList.size(); i++) {
                List list = lineList.get(i);
                String issue = Optional.ofNullable(list.get(13)).map(Object::toString).orElse(null);
                String handlerstatus = Optional.ofNullable(list.get(16)).map(Object::toString).orElse(null);
                final JsonNode issueJsonNode = JsonUtil.GLOBAL_OBJECT_MAPPER.readTree(issue);
                if (issueJsonNode == null) {
                    lineList.remove(i);
                    i--;
                    continue;
                }
                final JsonNode handlerstatusJsonNode = JsonUtil.GLOBAL_OBJECT_MAPPER.readTree(handlerstatus);
                String lastIssue = issueJsonNode.get(issueJsonNode.size() - 1).asText();
                String lastHandlerstatusJsonNode = handlerstatusJsonNode.get(handlerstatusJsonNode.size() - 1).asText();
                if (!"4".equals(lastHandlerstatusJsonNode)) {
                    //只处理已经给出终极意见的
                    lineList.remove(i);
                    i--;
                    continue;
                }
                int isAlarm = -1;
                if (lastIssue.contains("真报警") ||
                        lastIssue.contains("真实报警") ||
                        lastIssue.contains("真实故障")) {
                    isAlarm = 1;
                } else if (lastIssue.contains("误报警")) {
                    isAlarm = 0;
                } else {
                    logger.info(lastIssue);
                }
                list.add(isAlarm);
            }
            lineList.add(0, Arrays.asList(header));
            EasyExcel.write(response.getOutputStream()).sheet(0).doWrite(lineList);

        } catch (IOException ex) {
            throw BaseRuntimeException.getException(ex);
        }

    }
}
