package com.cesi.datalogscheduler.mapper;

import com.cesi.datalogscheduler.entity.BackupInfo;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

@Mapper
@Qualifier("cba")
public interface BackupInfoMapper {
    List<BackupInfo> findAll();

    int insert(List<BackupInfo> list);

    int delete(List<BackupInfo> list);

    int update(List<BackupInfo> list);
}
