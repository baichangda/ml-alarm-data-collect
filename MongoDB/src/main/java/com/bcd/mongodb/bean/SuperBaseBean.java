package com.bcd.mongodb.bean;


import com.fasterxml.jackson.annotation.JsonFilter;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/5/2.
 */
@Accessors(chain = true)
@Getter
@Setter
public abstract class SuperBaseBean<K extends Serializable> implements Serializable {
    /**
     * id取规则参考{@link org.springframework.data.mongodb.core.mapping.BasicMongoPersistentProperty#isIdProperty}
     *
     * 优先级为:
     * 1、{@link Id}注解字段
     * 2、名称为 id、_id 字段
     *
     */
    @ApiModelProperty(value = "主键(唯一标识符,自动生成)(不需要赋值)")
    @Id
    //主键
    public K id;

    @Override
    public int hashCode() {
        return id==null?0:id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(this==obj){
            return true;
        }else{
            if(obj==null){
                return false;
            }else{
                if(this.getClass()==obj.getClass()){
                    Object objId=((SuperBaseBean)obj).getId();
                    if(id==objId){
                        return true;
                    }else{
                        if(id==null||objId==null){
                            return false;
                        }else{
                            return id.equals(objId);
                        }
                    }
                }else{
                    return false;
                }
            }
        }
    }
}