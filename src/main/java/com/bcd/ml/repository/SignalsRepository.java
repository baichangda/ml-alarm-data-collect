package com.bcd.ml.repository;

import com.bcd.mongodb.repository.BaseRepository;
import org.springframework.stereotype.Repository;
import com.bcd.ml.bean.SignalsBean;


@Repository
public interface SignalsRepository extends BaseRepository<SignalsBean, String> {

}
