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
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class MlService {

    Logger logger = LoggerFactory.getLogger(MlService.class);

    ZoneOffset zoneOffset = ZoneOffset.of("+8");


    public int[] fetchAndSave() {
        List<Map<String, String>> alarmList = HBaseUtil.queryAlarms();
        List<Map<String, JsonNode>> signalList = new ArrayList<>();
        //检测重复的alarm
        Set<String> set1 = new HashSet<>();
        //检测相同vin和time、但是不同type的 alarm
        Set<String> set2 = new HashSet<>();
        //检测重复的signal
        Set<String> set3 = new HashSet<>();
        for (int i = 0; i < alarmList.size(); i++) {
            try {
                Map<String, String> alarm = alarmList.get(i);
                String vin = alarm.get("vin");
                String beginTime = alarm.get("beginTime");
                String alarmType = alarm.get("alarmType");
                String alarmLevel = alarm.get("alarmLevel");
                String key1 = vin + "-" + beginTime + "-" + alarmType;
                if ("3".equals(alarmLevel)||set1.contains(key1)) {
                    alarmList.remove(i);
                    i--;
                } else {
                    set1.add(key1);
                    String key2 = vin + "-" + beginTime;
                    if(!set2.contains(key2)){
                        set2.add(key2);
                        Date alarmTime = DateZoneUtil.stringToDate_second(beginTime);
                        LocalDateTime ldt = LocalDateTime.ofInstant(alarmTime.toInstant(), zoneOffset);
                        Date d1 = Date.from(ldt.plusSeconds(-100).toInstant(zoneOffset));
                        Date d2 = Date.from(ldt.plusSeconds(99).toInstant(zoneOffset));
                        List<String> signals = HBaseUtil.querySignals(vin, d1, d2);
                        if (signals.isEmpty()) {
                            alarmList.remove(i);
                            i--;
                        } else {
                            for (String signalJson : signals) {
                                JsonNode jsonNode = JsonUtil.GLOBAL_OBJECT_MAPPER.readTree(signalJson);
                                JsonNode json = JsonUtil.GLOBAL_OBJECT_MAPPER.readTree(jsonNode.get("json").asText());
                                long fileCreationTime = json.get("FileCreationTime").asLong();
                                String key3 = vin + "-" + fileCreationTime;
                                if (!set3.contains(key3)) {
                                    set3.add(key3);
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
                                        data.put("collectDate", LongNode.valueOf(fileCreationTime + i));
                                        data.put("vin", TextNode.valueOf(vin));
                                        dataList.add(data);
                                    }
                                    signalList.addAll(dataList);
                                }
                            }
                        }
                    }
                }

            } catch (IOException e) {
                logger.error("parse alarm error", e);
            }
        }

        //save
        String alarmFilePath = "alarm.txt";
        String signalsFilePath = "signal.txt";

        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(alarmFilePath))) {
            for (Map<String, String> alarm : alarmList) {
                bw.write(JsonUtil.toJson(alarm));
                bw.newLine();
            }
            bw.flush();
        } catch (IOException e) {
            throw BaseRuntimeException.getException(e);
        }
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(signalsFilePath))) {
            for (Map<String, JsonNode> signal : signalList) {
                bw.write(JsonUtil.toJson(signal));
                bw.newLine();
            }
            bw.flush();
        } catch (IOException e) {
            throw BaseRuntimeException.getException(e);
        }

        return new int[]{alarmList.size(), signalList.size()};
    }
}
