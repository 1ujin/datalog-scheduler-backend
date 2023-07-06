package com.cesi.datalogscheduler.mapper;

import com.cesi.datalogscheduler.entity.ConnectionInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ConnectionInfoMapper {
    List<ConnectionInfo> findAll();

    int insert(List<ConnectionInfo> list);

    int delete(List<ConnectionInfo> list);

    int update(List<ConnectionInfo> list);
}
