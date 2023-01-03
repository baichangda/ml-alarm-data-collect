package com.bcd.ml.controller;

import com.bcd.base.controller.BaseController;
import com.bcd.base.message.JsonMessage;
import com.bcd.ml.service.MlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/ml")
public class MlController extends BaseController {

    @Autowired
    MlService mlService;

    @RequestMapping(value = "/fetchAndSave", method = RequestMethod.GET)
    @Operation(description = "获取报警数据和信号数据并保存文件")
    @ApiResponse(responseCode = "200", description = "获取报警数据和信号数据并保存文件")
    public JsonMessage<int[]> fetchAndSave(@RequestParam String alarmStartTimeStr,@RequestParam String alarmEndTimeStr){
        return JsonMessage.success(mlService.fetchAndSave(alarmStartTimeStr,alarmEndTimeStr));
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
    public JsonMessage<Integer> saveToMongo_gb(){
        return JsonMessage.success(mlService.saveToMongo_gb());
    }

    @RequestMapping(value = "/saveToMongo_gb_targz", method = RequestMethod.GET)
    @Operation(description = "解析tar数据文件保存到mongo")
    @ApiResponse(responseCode = "200", description = "解析tar数据文件保存到mongo")
    public JsonMessage<Integer> saveToMongo_gb_targz(
            @RequestParam String tarFilePath,
            @RequestParam String collection
    ){
        return JsonMessage.success(mlService.saveToMongo_gb_targz(tarFilePath,collection));
    }


    @RequestMapping(value = "/parseAlarmTxt", method = RequestMethod.POST,consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(description = "解析报警文本")
    @ApiResponse(responseCode = "200", description = "解析报警文本",
            content =@Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE),
            headers = {@Header(name = HttpHeaders.CONTENT_DISPOSITION,description = "attachment;filename=parseAlarmTxt.xlsx")})
    public void parseAlarmTxt(@RequestParam MultipartFile alarmTxtFile, HttpServletResponse response){
        doBeforeResponseFile("parseAlarmTxt.xlsx",response);
        mlService.parseAlarmTxt(alarmTxtFile,response);
    }

}
