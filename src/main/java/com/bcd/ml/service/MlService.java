package com.bcd.ml.service;

import com.bcd.base.exception.BaseRuntimeException;
import com.bcd.base.util.DateZoneUtil;
import com.bcd.base.util.JsonUtil;
import com.bcd.config.hbase.HBaseUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MlService {

    Logger logger = LoggerFactory.getLogger(MlService.class);

    ZoneOffset zoneOffset = ZoneOffset.of("+8");

    @Value("${alarmStartTimeStr}")
    String alarmStartTimeStr;

    @Value("${workPoolSize}")
    int workPoolSize;

    @Value("${alarmSourcePath}")
    String alarmSourcePath;

    @Value("${signalSourcePath}")
    String signalSourcePath;

    @Autowired
    MongoTemplate mongoTemplate;

    public int[] saveToMongo() {

        AtomicInteger allAlarmCount = new AtomicInteger();
        AtomicInteger allSignalCount = new AtomicInteger();
        AtomicInteger alarmCount = new AtomicInteger();
        AtomicInteger signalCount = new AtomicInteger();
        int period = 5;
        ScheduledExecutorService monitorPool = Executors.newScheduledThreadPool(1);
        monitorPool.scheduleWithFixedDelay(() -> {
            int count1 = alarmCount.getAndSet(0);
            int count2 = signalCount.getAndSet(0);
            logger.info("count[{},{}] speed[{},{}]", allAlarmCount.get(), allSignalCount.get(), count1 / period, count2 / period);
        }, period, period, TimeUnit.SECONDS);

        int batch = 1000;
        ExecutorService alarmPool = Executors.newSingleThreadExecutor();
        alarmPool.execute(() -> {
            try (BufferedReader br = Files.newBufferedReader(Paths.get(alarmSourcePath))) {
                List<String> tempList = new ArrayList<>();
                String line;
                while ((line = br.readLine()) != null) {
                    tempList.add(line);
                    if (tempList.size() == batch) {
                        mongoTemplate.insert(tempList, "alarm_all");
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

        try {
            alarmPool.shutdown();
            while (!alarmPool.awaitTermination(60, TimeUnit.SECONDS)) {

            }
            monitorPool.shutdown();
            while (!monitorPool.awaitTermination(60, TimeUnit.SECONDS)) {

            }
        }catch (InterruptedException e){
            throw BaseRuntimeException.getException(e);
        }

        return new int[]{allAlarmCount.get(),allSignalCount.get()};

    }

    public int[] fetchAndSave() {

        List<Map<String, String>> alarmList = HBaseUtil.queryAlarms();
        int size1 = alarmList.size();
        logger.info("fetch alarm[{}]", size1);
        if (size1 == 0) {
            return new int[]{0, 0};
        }
        long alarmStartTime = DateZoneUtil.stringToDate_day(alarmStartTimeStr).getTime();
        alarmList.removeIf(e -> {
            String alarmLevel = e.get("alarmLevel");
            if ("3".equals(alarmLevel)) {
                return true;
            } else {
                long cur = DateZoneUtil.stringToDate_second(e.get("beginTime")).getTime();
                return cur < alarmStartTime;
            }
        });
        int size2 = alarmList.size();
        logger.info("fetch filter alarm[{}]", size2);
        if (size2 == 0) {
            return new int[]{0, 0};
        }


        AtomicInteger allProcessedCount = new AtomicInteger();
        AtomicInteger allProcessedAlarmCount = new AtomicInteger();
        AtomicInteger allProcessedSignalCount = new AtomicInteger();
        AtomicInteger processedCount = new AtomicInteger();
        AtomicInteger processedAlarmCount = new AtomicInteger();
        AtomicInteger processedSignalCount = new AtomicInteger();
        AtomicInteger saveAlarmCount = new AtomicInteger();
        AtomicInteger saveSignalCount = new AtomicInteger();
        ScheduledExecutorService monitorPool = Executors.newScheduledThreadPool(1);
        int period = 5;
        monitorPool.scheduleWithFixedDelay(() -> {
            int allProcessed = allProcessedCount.get();
            int allProcessedAlarm = allProcessedAlarmCount.get();
            int allProcessedSignal = allProcessedSignalCount.get();
            int processed = processedCount.getAndSet(0);
            int processedAlarm = processedAlarmCount.getAndSet(0);
            int processedSignal = processedSignalCount.getAndSet(0);
            int saveAlarm = saveAlarmCount.getAndSet(0);
            int saveSignal = saveSignalCount.getAndSet(0);
            logger.info("processed[{}/{},({},{})] processSpeed[{},({},{})]  saveSpeed[({},{})]"
                    , allProcessed, size2, allProcessedAlarm, allProcessedSignal, processed / period, processedAlarm / period, processedSignal / period, saveAlarm / period, saveSignal / period);
        }, period, period, TimeUnit.SECONDS);


        AtomicBoolean stop = new AtomicBoolean(false);


        ArrayBlockingQueue<Map<String, String>> alarmQueue = new ArrayBlockingQueue<>(5000);
        ArrayBlockingQueue<List<Map<String, JsonNode>>> signalQueue = new ArrayBlockingQueue<>(5000);
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
            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(alarmFilePath), StandardOpenOption.APPEND)) {
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
            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(signalsFilePath), StandardOpenOption.APPEND)) {
                while (!stop.get()) {
                    List<Map<String, JsonNode>> list = signalQueue.poll(3, TimeUnit.SECONDS);
                    if (list != null) {
                        for (Map<String, JsonNode> data : list) {
                            bw.write(JsonUtil.toJson(data));
                            bw.newLine();
                        }
                        bw.flush();
                        saveSignalCount.addAndGet(list.size());
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
                            Date alarmTime = DateZoneUtil.stringToDate_second(beginTime);
                            LocalDateTime ldt = LocalDateTime.ofInstant(alarmTime.toInstant(), zoneOffset);
                            Date d1 = Date.from(ldt.plusSeconds(-48).toInstant(zoneOffset));
                            Date d2 = Date.from(ldt.plusSeconds(47).toInstant(zoneOffset));
                            List<String> signals = HBaseUtil.querySignals(vin, d1, d2);
                            int signalSize = signals.size();
                            String key2 = vin + "-" + beginTime;
                            Integer old = map2.putIfAbsent(key2, signalSize);
                            if (old == null) {
                                if (signalSize > 0) {
                                    processedAlarmCount.incrementAndGet();
                                    allProcessedAlarmCount.incrementAndGet();
                                    alarmQueue.put(alarm);
                                    for (String signalJson : signals) {
                                        JsonNode jsonNode = JsonUtil.GLOBAL_OBJECT_MAPPER.readTree(signalJson);
                                        JsonNode json = JsonUtil.GLOBAL_OBJECT_MAPPER.readTree(jsonNode.get("json").asText());
                                        long fileCreationTime = json.get("FileCreationTime").asLong();
                                        String key3 = vin + "-" + fileCreationTime;
                                        if (!set3.contains(key3)) {
                                            synchronized (key3.intern()) {
                                                if (set3.contains(key3)) {
                                                    continue;
                                                } else {
                                                    set3.add(key3);
                                                }
                                            }
                                            List<JsonNode> groupList = new ArrayList<>();
                                            for (JsonNode group : json.get("channels")) {
                                                groupList.add(group.get("data"));
                                            }
                                            int dataSize = groupList.get(0).size();
                                            List<Map<String, JsonNode>> dataList = new ArrayList<>();
                                            for (int j = 0; j < dataSize; j++) {
                                                Map<String, JsonNode> data = new HashMap<>();
                                                for (JsonNode group : groupList) {
                                                    JsonNode cur = group.get(j);
                                                    cur.fields().forEachRemaining(stringJsonNodeEntry -> {
                                                        String key = stringJsonNodeEntry.getKey();
                                                        JsonNode value = stringJsonNodeEntry.getValue();
                                                        if (value.isArray()) {
                                                            int index = 0;
                                                            for (JsonNode node : value) {
                                                                data.put(key + "_" + index++, node);
                                                            }
                                                        } else {
                                                            data.put(key, value);
                                                        }
                                                    });
                                                }
                                                data.put("collectDate", LongNode.valueOf(fileCreationTime + j));
                                                data.put("vin", TextNode.valueOf(vin));
                                                dataList.add(data);
                                            }
                                            processedSignalCount.addAndGet(dataSize);
                                            allProcessedSignalCount.addAndGet(dataSize);
                                            signalQueue.put(dataList);
                                        }
                                    }
                                }
                            } else {
                                if (old > 0) {
                                    processedAlarmCount.incrementAndGet();
                                    allProcessedAlarmCount.incrementAndGet();
                                    alarmQueue.put(alarm);
                                }
                            }
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    throw BaseRuntimeException.getException(e);
                }
            });
        }


        //split alarm
        Iterator<Map<String, String>> it = alarmList.iterator();
        while (it.hasNext()) {
            processedCount.incrementAndGet();
            allProcessedCount.incrementAndGet();
            try {
                Map<String, String> alarm = it.next();
                String vin = alarm.get("vin");
                String beginTime = alarm.get("beginTime");
                String alarmType = alarm.get("alarmType");
                String key1 = vin + "-" + beginTime + "-" + alarmType;
                if (!set1.contains(key1)) {
                    set1.add(key1);
                    String key2 = vin + "-" + beginTime;
                    Integer val2 = map2.get(key2);
                    if (val2 == null) {
                        workQueue.put(alarm);
                    } else {
                        if (val2 > 0) {
                            processedAlarmCount.incrementAndGet();
                            allProcessedAlarmCount.incrementAndGet();
                            alarmQueue.put(alarm);
                        }
                    }
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

        logger.info("finish alarm[{}] signal[{}]", allProcessedAlarmCount.get(), allProcessedSignalCount.get());

        return new int[]{processedAlarmCount.get(), processedSignalCount.get()};
    }
}
