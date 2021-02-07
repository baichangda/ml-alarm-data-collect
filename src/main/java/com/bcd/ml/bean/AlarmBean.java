package com.bcd.ml.bean;

import com.bcd.mongodb.bean.SuperBaseBean;
import com.bcd.mongodb.code.freemarker.CodeGenerator;
import com.bcd.mongodb.code.freemarker.CollectionConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

@Accessors(chain = true)
@Getter
@Setter
@Document(collection = "alarm")
public class AlarmBean extends SuperBaseBean<String> {
    private String alarmLevel;
    private String beginTime;
    private String endTime;
    private String handlerStatus;
    private String issue;
    private String tag;
    private String vehicleType;
    private String vin;
    private String workModel;
    private String alarmName;
    private String handlerTime;
    private String mileage;
    private String alarmType;
    private String handlerUser;
    private String platformCode;
    private String platformName;
    private String saleStatus;

    public static void main(String[] args) {
        CollectionConfig config1=new CollectionConfig("alarm","报警",AlarmBean.class)
                .setNeedCreateControllerFile(false)
                .setNeedValidateSaveParam(false);
        CollectionConfig config2=new CollectionConfig("signals","信号",SignalsBean.class)
                .setNeedCreateControllerFile(false)
                .setNeedValidateSaveParam(false);
        CollectionConfig config3=new CollectionConfig("collect","报警整合数据",CollectBean.class)
                .setNeedCreateControllerFile(false)
                .setNeedValidateSaveParam(false);
        CodeGenerator.generate(config1);
        CodeGenerator.generate(config2);
        CodeGenerator.generate(config3);
    }
}
