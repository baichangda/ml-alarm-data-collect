package com.bcd.ml.bean;

import com.bcd.mongodb.bean.SuperBaseBean;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

@Accessors(chain = true)
@Getter
@Setter
@Document(collection = "signals")
public class SignalsBean extends SuperBaseBean<String> {
    private long bMSTotalDTC;
    private String cellCellID;
    private double gnssAlt;
    private long hdop;
    private long tMTotalDTC;
    private double tboxAccelX;
    private double volt_0;
    private double accelActuPos;
    private String vin;
    private long gnssHead;
    private double tboxAccelY;
    private double gnssLat;
    private double tboxAccelZ;
    private double gnssLong;
    private double tem_0;
    private long gnssSats;
    private long gnssTime;
    private long cellSignalStrength;
    private long gpsStatus;
    private long collectDate;
    private long vehBMSBscSta;
    private double vehBatt;
    private double vehBrakePos;
    private long vehBrkFludLvlLow;
    private long vehBrkSysBrkLghtsReqd;
    private long vehBrkSysRedBrkTlltReq;
    private long vehCoolantTemp;
    private long vehCruiseActive;
    private long vehCruiseEnabled;
    private long vehCruiseTargetSpeed;
    private long vehGearPos;
    private long vehHorn;
    private long vehIndLightLeft;
    private long vehIndLightRight;
    private long vehInsideTemp;
    private long vehNightDetected;
    private double vehOdo;
    private long vehOdoV;
    private long vehOutsideTemp;
    private long vehRainDetected;
    private long vehSpdAvgDrvnV;
    private double vehSpeed;
    private double vehSteeringAngle;
    private long vehSunroof;
    private long vehSysPwrMod;
    private long vehTrShftLvrPosV;
    private long vehWindowFrontLeft;
    private long vehWindowFrontRight;
    private long vehWindowRearLeft;
    private long vehWindowRearRight;
    private long vehWiperSwitchFront;
    private long vehWorkModel_g;
    private String vehicleType;
    private long vehVSESts;
    private String cellChanID;
    private double volt_1;
    private double tem_1;
    private String dTCInfomationBMS;
    private double vehBMSCellMaxTem;
    private double vehChargerHVCurrent;
    private long vehEPBSysBrkLghtsReqd;
    private long vehHVDCDCSta;
    private double vehTMActuToq;
    private double volt_10;
    private double tem_10;
    private long vehBMSCellMaxTemIndx;
    private long vehTMActuToqV;
    private String cellLAC;
    private String dTCInfomationTC;
    private double vehChargerHVVolt;
    private long vehEPTAccelActuPosV;
    private long vehHVDCDCTem;
    private double volt_11;
    private double tem_11;
    private long vehBMSCellMaxTemV;
    private long vehEPTBrkPdlDscrtInptSts;
    private long vehTMInvtrCrnt;
    private String cellMCC;
    private long vehEPTBrkPdlDscrtInptStsV;
    private long vehTMInvtrCrntV;
    private double volt_12;
    private double tem_12;
    private double vehBMSCellMaxVol;
    private String cellMNC;
    private long vehBMSCellMaxVolIndx;
    private long vehEPTRdy;
    private long vehTMInvtrTem;
    private double volt_13;
    private double tem_13;
    private long cellRAT;
    private long vehBMSCellMaxVolV;
    private double vehEPTTrInptShaftToq;
    private double vehTMInvtrVol;
    private double volt_14;
    private double tem_14;
    private long vehEPTTrInptShaftToqV;
    private long vehTMInvtrVolV;
    private double vehBMSCellMinTem;
    private double volt_15;
    private double tem_15;
    private long vehBMSCellMinTemIndx;
    private long vehTMSpd;
    private long vehTMSpdV;
    private double volt_16;
    private long vehBMSCellMinTemV;
    private double tem_2;
    private long vehTMSta;
    private double volt_17;
    private double vehBMSCellMinVol;
    private double tem_3;
    private long vehBMSCellMinVolIndx;
    private long vehTMSttrTem;
    private double volt_18;
    private double tem_4;
    private long vehBMSCellMinVolV;
    private double volt_19;
    private double tem_5;
    private long vehBMSHVILClsd;
    private double volt_2;
    private double tem_6;
    private double vehBMSPackCrnt;
    private double volt_20;
    private long vehBMSPackCrntV;
    private double tem_7;
    private double volt_21;
    private double vehBMSPackSOC;
    private double tem_8;
    private long vehBMSPackSOCV;
    private double volt_22;
    private double tem_9;
    private double vehBMSPackVol;
    private double volt_23;
    private long vehBMSPackVolV;
    private long vehABSF;
    private double volt_24;
    private double vehBMSPtIsltnRstc;
    private long vehAC;
    private long vehBMSPtIsltnRstcV;
    private double volt_25;
    private long vehACAuto;
    private double volt_26;
    private long vehACCircDirection;
    private double volt_27;
    private long vehACCircType;
    private double volt_28;
    private long vehACDrvTargetTemp;
    private double volt_29;
    private long vehACFanSpeed;
    private long vehACPassTargetTemp;
    private double volt_3;
    private double volt_30;
    private double volt_31;
    private double volt_32;
    private double volt_33;
    private double volt_34;
    private double volt_35;
    private double volt_36;
    private double volt_37;
    private double volt_38;
    private double volt_39;
    private double volt_4;
    private double volt_40;
    private double volt_41;
    private double volt_42;
    private double volt_43;
    private double volt_44;
    private double volt_45;
    private double volt_46;
    private double volt_47;
    private double volt_48;
    private double volt_49;
    private double volt_5;
    private double volt_50;
    private double volt_51;
    private double volt_52;
    private double volt_53;
    private double volt_54;
    private double volt_55;
    private double volt_56;
    private double volt_57;
    private double volt_58;
    private double volt_59;
    private double volt_6;
    private double volt_60;
    private double volt_61;
    private double volt_62;
    private double volt_63;
    private double volt_64;
    private double volt_65;
    private double volt_66;
    private double volt_67;
    private double volt_68;
    private double volt_69;
    private double volt_7;
    private double volt_70;
    private double volt_71;
    private double volt_72;
    private double volt_73;
    private double volt_74;
    private double volt_75;
    private double volt_76;
    private double volt_77;
    private double volt_78;
    private double volt_79;
    private double volt_8;
    private double volt_80;
    private double volt_81;
    private double volt_82;
    private double volt_83;
    private double volt_84;
    private double volt_85;
    private double volt_86;
    private double volt_87;
    private double volt_88;
    private double volt_89;
    private double volt_9;
    private double volt_90;
    private double volt_91;
    private double volt_92;
    private double volt_93;
    private double volt_94;
    private double volt_95;
}