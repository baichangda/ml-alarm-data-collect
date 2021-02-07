package com.bcd.ml.bean;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Accessors(chain = true)
@Getter
@Setter
public class CollectBean{
    private String id;
    private AlarmBean alarm;
    private List<SignalsBean> signals;
}
