package com.bcd.ml.controller;

import com.bcd.base.message.JsonMessage;
import com.bcd.ml.service.MlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ml")
public class MlController {

    @Autowired
    MlService mlService;

    @RequestMapping(value = "/fetchAndSave", method = RequestMethod.GET)
    @Operation(description = "获取报警数据和信号数据并保存文件")
    @ApiResponse(responseCode = "200", description = "获取报警数据和信号数据并保存文件")
    public JsonMessage<int[]> fetchAndSave(@RequestParam int flag,@RequestParam String alarmStartTimeStr){
        return JsonMessage.success(mlService.fetchAndSave(flag,alarmStartTimeStr));
    }

    @RequestMapping(value = "/saveToMongo", method = RequestMethod.GET)
    @Operation(description = "解析数据文件保存到mongo")
    @ApiResponse(responseCode = "200", description = "解析数据文件保存到mongo")
    public JsonMessage<int[]> saveToMongo(@RequestParam String alarmCollection,@RequestParam(required = false) String signalCollection){
        return JsonMessage.success(mlService.saveToMongo(alarmCollection,signalCollection));
    }

    @RequestMapping(value = "/fetchAndSave_gb", method = RequestMethod.GET)
    @Operation(description = "获取国标信号数据并保存文件")
    @ApiResponse(responseCode = "200", description = "获取报警数据和信号数据并保存文件")
    public JsonMessage<Integer> fetchAndSave_gb(@RequestParam(defaultValue = "10000000") int num){
        return JsonMessage.success(mlService.fetchAndSave_gb(num));
    }

    @RequestMapping(value = "/saveToMongo_gb", method = RequestMethod.GET)
    @Operation(description = "解析数据文件保存到mongo")
    @ApiResponse(responseCode = "200", description = "解析数据文件保存到mongo")
    public JsonMessage<Integer> saveToMongo(){
        return JsonMessage.success(mlService.saveToMongo_gb());
    }
}
