package com.bcd.ml.repository;

import com.bcd.mongodb.repository.BaseRepository;
import org.springframework.stereotype.Repository;
import com.bcd.ml.bean.AlarmBean;


@Repository
public interface AlarmRepository extends BaseRepository<AlarmBean, String> {

}
