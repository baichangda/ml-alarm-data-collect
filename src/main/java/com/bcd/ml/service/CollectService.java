package com.bcd.ml.service;

import com.bcd.base.condition.Condition;
import com.bcd.base.condition.impl.NumberCondition;
import com.bcd.base.condition.impl.StringCondition;
import com.bcd.base.exception.BaseRuntimeException;
import com.bcd.base.util.JsonUtil;
import com.bcd.ml.bean.AlarmBean;
import com.bcd.ml.bean.CollectAvgBean;
import com.bcd.ml.bean.CollectBean;
import com.bcd.ml.bean.SignalsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class CollectService {

    static Field[] signalsBeanNumberFields;
    static Field[] collectAvgBeanNumberFields;

    static {
        signalsBeanNumberFields = Arrays.stream(SignalsBean.class.getDeclaredFields())
                .filter(e -> {
                    e.setAccessible(true);
                    Class<?> type = e.getType();
                    boolean isNumber = Number.class.isAssignableFrom(type) ||
                            Double.TYPE.isAssignableFrom(type) ||
                            Float.TYPE.isAssignableFrom(type) ||
                            Long.TYPE.isAssignableFrom(type) ||
                            Integer.TYPE.isAssignableFrom(type) ||
                            Short.TYPE.isAssignableFrom(type) ||
                            Byte.TYPE.isAssignableFrom(type);
                    //去除date、time 时间字段
                    String fieldName = e.getName();
                    return isNumber && !fieldName.contains("Time") && !fieldName.contains("Date");
                }).toArray(Field[]::new);
        collectAvgBeanNumberFields = Arrays.stream(signalsBeanNumberFields).map(e -> {
            try {
                Field declaredField = CollectAvgBean.class.getDeclaredField(e.getName());
                declaredField.setAccessible(true);
                return declaredField;
            } catch (NoSuchFieldException ex) {
                throw BaseRuntimeException.getException(ex);
            }
        }).toArray(Field[]::new);
    }

    Logger logger = LoggerFactory.getLogger(CollectService.class);
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    AlarmService alarmService;
    @Autowired
    SignalsService signalsService;

    /**
     * 取每条报警数据的 前15s-后14s 数据
     * 同时去掉 valid 结尾的数据
     * 生成{@link CollectBean}
     *
     * @return
     */
    public int collect(String collectionName) {
        mongoTemplate.remove(new Query(), collectionName);
        int count = 0;
        int pageNum = 0;
        int pageSize = 1000;
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        while (true) {
            Page<AlarmBean> page = alarmService.findAll(pageable);
            List<AlarmBean> content = page.getContent();
            List<CollectBean> collectBeans = new ArrayList<>();
            for (AlarmBean alarmBean : content) {
                CollectBean collectBean = toCollectBean(alarmBean);
                if (collectBean == null) {
                    logger.info("alarm id[{}] vin[{}] beginTime[{}] endTime[{}] has not signals"
                            , alarmBean.getId(), alarmBean.getVin(),
                            alarmBean.getBeginTime(), alarmBean.getEndTime());
                } else {
                    collectBeans.add(collectBean);
                }
            }
            if (!collectBeans.isEmpty()) {
                mongoTemplate.insert(collectBeans, collectionName);
                count += collectBeans.size();
            }

            if (page.hasNext()) {
                pageNum++;
                pageable = PageRequest.of(pageNum, pageSize);
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * 取每条报警数据的 前15s-后14s 数据
     * 同时去掉 valid 结尾的数据
     * 将{@link SignalsBean}中每一秒的字段扩展到生成的对象中
     *
     * @return map
     */
    public int collectWithExtFields(String collectionName) {
        mongoTemplate.remove(new Query(), collectionName);
        int count = 0;
        int pageNum = 0;
        int pageSize = 1000;
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        while (true) {
            Page<AlarmBean> page = alarmService.findAll(pageable);
            List<AlarmBean> content = page.getContent();
            List<Map<String, Object>> dataList = new ArrayList<>();
            for (AlarmBean alarmBean : content) {
                CollectBean collectBean = toCollectBean(alarmBean);
                if (collectBean == null) {
                    logger.info("alarm id[{}] vin[{}] beginTime[{}] endTime[{}] has not signals"
                            , alarmBean.getId(), alarmBean.getVin(),
                            alarmBean.getBeginTime(), alarmBean.getEndTime());
                } else {
                    Map<String, Object> dataMap = JsonUtil.GLOBAL_OBJECT_MAPPER.convertValue(alarmBean, Map.class);
                    long startTime = Long.parseLong(alarmBean.getBeginTime());
                    List<SignalsBean> signalsBeanList = collectBean.getSignals();
                    try {
                        for (SignalsBean signalsBean : signalsBeanList) {
                            long diff = signalsBean.getCollectDate() - startTime + 15;
                            for (Field signalsBeanNumberField : signalsBeanNumberFields) {
                                dataMap.put(signalsBeanNumberField.getName() + "__" + diff, signalsBeanNumberField.get(signalsBean));
                            }
                        }
                    } catch (IllegalAccessException e) {
                        throw BaseRuntimeException.getException(e);
                    }
                    dataList.add(dataMap);
                }
            }
            if (!dataList.isEmpty()) {
                mongoTemplate.insert(dataList, collectionName);
                count += dataList.size();
            }

            if (page.hasNext()) {
                pageNum++;
                pageable = PageRequest.of(pageNum, pageSize);
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * 取每条报警数据的 前15s-后14s 数据
     * 同时去掉 valid 结尾的数据
     * 最后对所有{@link SignalsBean}中的数字类型求平均值
     * 生成{@link CollectAvgBean}
     *
     * @return
     */
    public int collectWithAvg(String collectionName) {
        mongoTemplate.remove(new Query(), collectionName);
        int count = 0;
        int pageNum = 0;
        int pageSize = 1000;
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        while (true) {
            Page<AlarmBean> page = alarmService.findAll(pageable);
            List<AlarmBean> content = page.getContent();
            List<CollectAvgBean> collectBeans = new ArrayList<>();
            for (AlarmBean alarmBean : content) {
                List<SignalsBean> signalsBeanList = getSignals(alarmBean);
                CollectAvgBean collectAvgBean = toCollectAvgBean(alarmBean, signalsBeanList);
                if (collectAvgBean == null) {
                    logger.info("alarm id[{}] vin[{}] beginTime[{}] endTime[{}] has not signals"
                            , alarmBean.getId(), alarmBean.getVin(),
                            alarmBean.getBeginTime(), alarmBean.getEndTime());
                } else {
                    collectBeans.add(collectAvgBean);
                }
            }
            if (!collectBeans.isEmpty()) {
                mongoTemplate.insert(collectBeans, collectionName);
                count += collectBeans.size();
            }

            if (page.hasNext()) {
                pageNum++;
                pageable = PageRequest.of(pageNum, pageSize);
            } else {
                break;
            }
        }
        return count;
    }

    private List<SignalsBean> getSignals(AlarmBean alarmBean) {
        long startTime = Long.parseLong(alarmBean.getBeginTime());
        Condition condition = Condition.and(
                new StringCondition("vin", alarmBean.getVin()),
                new NumberCondition("collectDate", startTime - 15, NumberCondition.Handler.GE),
                new NumberCondition("collectDate", startTime + 14, NumberCondition.Handler.LE)
        );
        return signalsService.findAll(condition);
    }

    private CollectBean toCollectBean(AlarmBean alarmBean) {
        List<SignalsBean> signalsBeanList = getSignals(alarmBean);
        if (!signalsBeanList.isEmpty()) {
            CollectBean collectBean = new CollectBean();
            collectBean.setAlarm(alarmBean);
            collectBean.setSignals(signalsBeanList);
            return collectBean;
        } else {
            return null;
        }
    }

    private CollectAvgBean toCollectAvgBean(AlarmBean alarmBean, List<SignalsBean> signalsBeans) {
        if (signalsBeans.isEmpty()) {
            return null;
        }
        CollectAvgBean collectAvgBean = new CollectAvgBean();
        BeanUtils.copyProperties(alarmBean, collectAvgBean);
        BigDecimal[] vals = new BigDecimal[signalsBeanNumberFields.length];
        try {
            for (SignalsBean signal : signalsBeans) {
                for (int i = 0; i < signalsBeanNumberFields.length; i++) {
                    Number val = (Number) signalsBeanNumberFields[i].get(signal);
                    if (vals[i] == null) {
                        vals[i] = BigDecimal.valueOf(val.doubleValue());
                    } else {
                        vals[i] = vals[i].add(BigDecimal.valueOf(val.doubleValue()));
                    }
                }
            }

            for (int i = 0; i < collectAvgBeanNumberFields.length; i++) {
                BigDecimal val = vals[i];
                collectAvgBeanNumberFields[i].set(collectAvgBean, val.divide(BigDecimal.valueOf(vals.length), 2, BigDecimal.ROUND_HALF_UP).doubleValue());
            }
        } catch (IllegalAccessException ex) {
            throw BaseRuntimeException.getException(ex);
        }
        return collectAvgBean;
    }


}
