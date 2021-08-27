package com.bcd.ml.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.Cell;
import com.bcd.base.exception.BaseRuntimeException;
import com.bcd.ml.bean.ConfusionMatrixAndRocResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ResultAnalysisService {


    Logger logger= LoggerFactory.getLogger(ResultAnalysisService.class);

    public ConfusionMatrixAndRocResult confusionMatrixAndRoc(MultipartFile file) {
        AtomicInteger realTrue_predictTrue=new AtomicInteger();
        AtomicInteger realTrue_predictFalse=new AtomicInteger();
        AtomicInteger realFalse_predictTrue=new AtomicInteger();
        AtomicInteger realFalse_predictFalse=new AtomicInteger();

        try {
            final InputStream is = file.getInputStream();
            EasyExcel.read(is).sheet(0).registerReadListener(new AnalysisEventListener() {
                @Override
                public void invoke(Object data, AnalysisContext context) {
                    LinkedHashMap<Integer,String> map=(LinkedHashMap<Integer,String>)data;
                    String realRes=map.get(3);
                    String predictRes=map.get(4);
                    if(realRes!=null&&predictRes!=null){
                        if(!predictRes.contains("无")){
                            if(realRes.contains("真")&&predictRes.contains("真")){
                                realTrue_predictTrue.incrementAndGet();
                            }else if(realRes.contains("真")&&predictRes.contains("假")){
                                realTrue_predictFalse.incrementAndGet();
                            }else if(realRes.contains("假")&&predictRes.contains("真")){
                                realFalse_predictTrue.incrementAndGet();
                            }else{
                                realFalse_predictFalse.incrementAndGet();
                            }
                        }
                        logger.info("{},{}",realRes,predictRes);
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    logger.info("finish read excel realTrue_predictTrue[{}] realTrue_predictFalse[{}] realFalse_predictTrue[{}] realFalse_predictFalse[{}]"
                            ,realTrue_predictTrue.get(),realTrue_predictFalse.get(),realFalse_predictTrue.get(),realFalse_predictFalse.get());
                }
            }).doRead();
            final ConfusionMatrixAndRocResult confusionMatrixAndRocResult = new ConfusionMatrixAndRocResult(realTrue_predictTrue.get(),realTrue_predictFalse.get(),realFalse_predictTrue.get(),realFalse_predictFalse.get());
            confusionMatrixAndRocResult.calc();
            return confusionMatrixAndRocResult;
        } catch (IOException e) {
            throw BaseRuntimeException.getException(e);
        }
    }
}
