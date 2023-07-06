package com.cesi.datalogscheduler.mapper;

import com.cesi.datalogscheduler.entity.ResultCount;
import com.cesi.datalogscheduler.entity.ResultInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResultInfoMapper {
    List<ResultInfo> findAll(int limit, int offset, String beginDate, String endDate, List<String> testerNames, String computerName);

    int countAll(String beginDate, String endDate, List<String> testerNames, String computerName);

    int insert(ResultInfo resultInfo);

    int multiInsert(List<ResultInfo> list);

    List<ResultInfo> duplicateFilename(List<String> testerNames, String computerName, String beginDate, String endDate);

    List<ResultCount> duplicateFilenameCount(List<String> testerNames, String model, String computerName, String beginDate, String endDate);

    List<ResultInfo> errorContent(List<String> testerNames, String computerName, String beginDate, String endDate);

    List<ResultCount> errorContentCount(List<String> testerNames, String model, String computerName, String beginDate, String endDate);

    List<ResultInfo> passedToFailed(List<String> testerNames, String computerName, String beginDate, String endDate);

    List<ResultCount> passedToFailedCount(List<String> testerNames, String model, String computerName, String beginDate, String endDate);

    List<ResultInfo> failedToPassed(List<String> testerNames, String computerName, String beginDate, String endDate);

    List<ResultCount> failedToPassedCount(List<String> testerNames, String model, String computerName, String beginDate, String endDate);

    List<ResultInfo> testSuiteError(List<String> testerNames, String model, String computerName, String beginDate, String endDate);

    List<ResultCount> testSuiteErrorCount(List<String> testerNames, String model, String computerName, String beginDate, String endDate);

    List<ResultInfo> filesizeError(List<String> testerNames, String model, String computerName, String beginDate, String endDate);

    List<ResultCount> filesizeErrorCount(List<String> testerNames, String model, String computerName, String beginDate, String endDate);

    List<ResultInfo> missReport(List<String> testerNames, String computerName, String beginDate, String endDate);

    List<ResultCount> missReportCount(List<String> testerNames, String model, String computerName, String beginDate, String endDate);

    List<ResultInfo> missProcess(List<String> testerNames, String computerName, String beginDate, String endDate);

    List<ResultCount> missProcessCount(List<String> testerNames, String model, String computerName, String beginDate, String endDate);

    List<ResultCount> countByDay(List<String> testerNames, String computerName, String beginDate, String endDate);

    List<ResultCount> countByWeek(List<String> testerNames, String computerName, String beginDate, String endDate);

    List<ResultCount> countByMonth(List<String> testerNames, String computerName, String beginDate, String endDate);

    List<String> getAllModels();

    List<ResultInfo> findDuplicate(@Param("resultInfo") ResultInfo resultInfo);

    List<ResultInfo> multiFindDuplicate(List<ResultInfo> list);
}
