package com.cesi.datalogscheduler.mapper;

import com.cesi.datalogscheduler.entity.Tester;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TesterMapper {
    List<Tester> findAll();

    int insert(List<Tester> list);

    int delete(List<Tester> list);

    int update(List<Tester> list);
}
