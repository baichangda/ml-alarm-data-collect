package com.bcd.ml.controller;

import com.bcd.base.controller.BaseController;
import com.bcd.base.message.JsonMessage;
import com.bcd.ml.service.CollectService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MLController extends BaseController {

    @Autowired
    CollectService collectService;

    @ApiOperation(value = "整合报警数据", notes = "整合报警数据")
    @ApiResponse(code = 200, message = "整合数据条数")
    @GetMapping("/api/ml/collect")
    public JsonMessage<Integer> collect(@ApiParam @RequestParam String collectionName){
        return JsonMessage.success().withData(collectService.collect(collectionName));
    }

    @ApiOperation(value = "整合报警数据(求数值平均)", notes = "整合报警数据(求数值平均)")
    @ApiResponse(code = 200, message = "整合数据条数")
    @GetMapping("/api/ml/collectWithAvg")
    public JsonMessage<Integer> collectWithAvg(@ApiParam @RequestParam String collectionName){
        return JsonMessage.success().withData(collectService.collectWithAvg(collectionName));
    }
}
