package com.bcd.ml.controller;

import com.bcd.base.message.JsonMessage;
import com.bcd.ml.bean.ConfusionMatrixAndRocResult;
import com.bcd.ml.service.ResultAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/resultAnalysis")
public class ResultAnalysisController {

    @Autowired
    ResultAnalysisService resultAnalysisService;

    @RequestMapping(value = "/confusionMatrixAndRoc", method = RequestMethod.POST,consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(description = "获取混淆矩阵和roc")
    @ApiResponse(responseCode = "200", description = "获取混淆矩阵和roc")
    public JsonMessage<ConfusionMatrixAndRocResult> fetchAndSave(@RequestParam MultipartFile file){
        return JsonMessage.success(resultAnalysisService.confusionMatrixAndRoc(file));
    }
}
