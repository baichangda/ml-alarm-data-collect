package com.bcd.ml.controller;

import com.bcd.base.message.JsonMessage;
import com.bcd.ml.service.MlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ml")
public class MlController {

    @Autowired
    MlService mlService;

    @RequestMapping(value = "/fetchAndSave", method = RequestMethod.GET)
    @Operation(description = "获取报警数据和信号数据并保存文件")
    @ApiResponse(responseCode = "200", description = "获取报警数据和信号数据并保存文件")
    public JsonMessage<int[]> fetchAndSave(){
        return JsonMessage.<int[]>success().withData(mlService.fetchAndSave());
    }

    @RequestMapping(value = "/fetchAndSave2", method = RequestMethod.GET)
    @Operation(description = "获取报警数据和信号数据并保存文件")
    @ApiResponse(responseCode = "200", description = "获取报警数据和信号数据并保存文件")
    public JsonMessage<int[]> fetchAndSave2(){
        return JsonMessage.<int[]>success().withData(mlService.fetchAndSave2());
    }

    @RequestMapping(value = "/saveToMongo", method = RequestMethod.GET)
    @Operation(description = "解析数据文件保存到mongo")
    @ApiResponse(responseCode = "200", description = "解析数据文件保存到mongo")
    public JsonMessage<int[]> saveToMongo(){
        return JsonMessage.<int[]>success().withData(mlService.saveToMongo());
    }
}
