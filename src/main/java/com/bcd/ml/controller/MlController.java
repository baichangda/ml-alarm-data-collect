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
    @Operation(description = "获取报警数据和信号数据并保存")
    @ApiResponse(responseCode = "200", description = "获取报警数据和信号数据并保存")
    public JsonMessage<int[]> fetchAndSave(){
        return JsonMessage.<int[]>success().withData(mlService.fetchAndSave());
    }
}
