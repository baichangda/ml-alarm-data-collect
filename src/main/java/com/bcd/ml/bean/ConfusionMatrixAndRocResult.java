package com.bcd.ml.bean;

import lombok.Data;

@Data
public class ConfusionMatrixAndRocResult {
    private float roc;

    private float true_precision;
    private float true_recall;
    private float true_f1Score;
    private float true_support;

    private float false_precision;
    private float false_recall;
    private float false_f1Score;
    private float false_support;

    private int realTrue_predictTrue;
    private int realTrue_predictFalse;
    private int realFalse_predictTrue;
    private int realFalse_predictFalse;

    public ConfusionMatrixAndRocResult(int realTrue_predictTrue, int realTrue_predictFalse, int realFalse_predictTrue, int realFalse_predictFalse) {
        this.realTrue_predictTrue = realTrue_predictTrue;
        this.realTrue_predictFalse = realTrue_predictFalse;
        this.realFalse_predictTrue = realFalse_predictTrue;
        this.realFalse_predictFalse = realFalse_predictFalse;
    }

    public void calc() {
        true_precision = ((float) realTrue_predictTrue) / (realTrue_predictTrue + realFalse_predictTrue);
        true_recall = ((float) realTrue_predictTrue) / (realTrue_predictTrue + realFalse_predictFalse);
        true_f1Score = 2 * true_precision * true_recall / (true_precision + true_recall);

        false_precision = ((float) realFalse_predictFalse) / (realTrue_predictFalse + realFalse_predictFalse);
        false_recall = ((float) realFalse_predictFalse) / (realTrue_predictTrue + realFalse_predictFalse);
        false_f1Score = 2 * false_precision * false_recall / (false_precision + false_recall);
    }
}
