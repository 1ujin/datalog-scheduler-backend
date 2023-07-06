package com.cesi.datalogscheduler.component;

import com.cesi.datalogscheduler.entity.ResultInfo;
import com.cesi.datalogscheduler.enums.AgingPhase;
import com.cesi.datalogscheduler.enums.ResultEnum;
import com.cesi.datalogscheduler.enums.SystemEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class Statistics {

    private static final String TEMPERATURE_REGEX = "(?<=/)(-55|-40|0|25|85|105|125)(?![0-9])(?=.*$)";
    private static final String LABEL_RESULT_J750UF = "\\s+Site\\s+Sort\\s+Bin\\s*";
    private static final String LABEL_PASSED_J750UF = "\\s+0\\s+1\\s+1\\s*";
    private static final String LABEL_BEGIN_TESTSUITE_93000 = "(?<==\\sStarted Testsuite\\s).+(?=\\s=)";
    private static final String LABEL_END_TESTSUITE_93000 = "[=]+\\sEnded Testsuite\\s.+\\s[=]+\\s*";
    private static final String LABEL_X_TESTSUITE_93000 = "(?<=Test Suite\\s).*(?=\\s(PASSED|FAILED))";
    private static final String FILENAME_J750UF = "^[0-9]+(_(?i)failed)?\\.(?i)txt";
    private static final String FILENAME_93000 = "^[0-9]+(_(?i)x)?(_(?i)failed)?\\.(?i)txt";
    private static final String PATH_REGEX = "/[0-9]+_DATA/[0-9]+/(?i)(laolianqian|laolianhou/20[0-9]{6}|JD)/20[0-9]{6}(_.*)?_[A-Z,a-z]{2,4}/[^0-9_]{0,2}[0-9]+(_(?i)x)?(_(?i)failed)?\\.(?i)txt";

    private static final Pattern PATTERN_TEMPERATURE_REGEX = Pattern.compile(TEMPERATURE_REGEX);
    private static final Pattern PATTERN_LABEL_RESULT_J750UF = Pattern.compile(LABEL_RESULT_J750UF);
    private static final Pattern PATTERN_LABEL_PASSED_J750UF = Pattern.compile(LABEL_PASSED_J750UF);
    private static final Pattern PATTERN_LABEL_BEGIN_TESTSUITE_93000 = Pattern.compile(LABEL_BEGIN_TESTSUITE_93000);
    private static final Pattern PATTERN_LABEL_END_TESTSUITE_93000 = Pattern.compile(LABEL_END_TESTSUITE_93000);
    private static final Pattern PATTERN_LABEL_X_TESTSUITE_93000 = Pattern.compile(LABEL_X_TESTSUITE_93000);
    private static final Pattern PATTERN_FILENAME_J750UF = Pattern.compile(FILENAME_J750UF);
    private static final Pattern PATTERN_FILENAME_93000 = Pattern.compile(FILENAME_93000);
    private static final Pattern PATTERN_PATH_REGEX = Pattern.compile(PATH_REGEX);

    /**
     * 解析文件路径信息
     *
     * @param filename  文件名
     * @param duplicate 重复文件名
     * @param prefix    文件路径前缀
     * @param system    测试系统
     */
    public ResultInfo parse(String filename, @Nullable String duplicate, String prefix, @NonNull SystemEnum system, String computerName) {
        // 判断路径是否合规
        if (!PATTERN_PATH_REGEX.matcher(filename).find()) {
            return null;
        }
        Path path = Path.of(filename);
        Path prefixPath = Path.of(prefix);
        int begin = prefixPath.getNameCount();
        // 型号
        String model = path.getName(begin).toString();
        // 批次
        String batch = path.getName(begin + 1).toString();
        // 老炼阶段
        AgingPhase agingPhase = AgingPhase.ofName(path.getName(begin + 2).toString()).orElse(null);
        // 老炼结束时间
        LocalDate agingEndTime = null;
        if (AgingPhase.LAOLIANHOU == agingPhase) {
            agingEndTime = LocalDate.parse(path.getName(begin + 3).toString(), DateTimeFormatter.BASIC_ISO_DATE);
        }
        String[] testerGroup = path.getName(path.getNameCount() - 2).toString().split("_");
        // 测试开始时间
        LocalDate testBeginTime = LocalDate.parse(testerGroup[0], DateTimeFormatter.BASIC_ISO_DATE);
        // 温度
        Integer temperature = null;
        // 鉴定分组
        String jdGroup = null;
        if (AgingPhase.LAOLIANHOU == agingPhase) {
            temperature = Integer.valueOf(testerGroup[1].replace("C", ""));
        } else if (AgingPhase.JD == agingPhase) {
            jdGroup = testerGroup[1];
        }
        // 测试人员姓名
        String testerName = testerGroup[testerGroup.length - 1];
        String chipId = path.getFileName().toString().split("(\\.)|_")[0];

        ResultInfo resultInfo = new ResultInfo();
        resultInfo.setModel(model.replace("_DATA", ""));
        resultInfo.setBatch(batch);
        if (agingPhase != null) {
            resultInfo.setAgingPhase(agingPhase.name());
        }
        resultInfo.setAgingEndTime(agingEndTime);
        resultInfo.setTestBeginTime(testBeginTime);
        resultInfo.setTemperature(temperature);
        resultInfo.setJdGroup(jdGroup);
        resultInfo.setTesterNameAbbr(testerName);
        resultInfo.setChipId(chipId);
        resultInfo.setPath(filename.replace("\\", "/"));
        if (duplicate != null) {
            resultInfo.setDuplicatePath(duplicate.replace("\\", "/"));
        }
        resultInfo.setComputerName(computerName);
        switch (system) {
            case J750:
                resultInfo.setSystem(SystemEnum.J750.name());
                break;
            case ULTRA_FLEX:
                resultInfo.setSystem(SystemEnum.ULTRA_FLEX.name());
                break;
            case ADV93000:
                resultInfo.setSystem(SystemEnum.ADV93000.name());
                break;
        }
        analyse(path, resultInfo);
        return resultInfo;
    }

    /**
     * 分析文件内容
     *
     * @param path       文件名
     * @param resultInfo 结果信息
     */
    public void analyse(Path path, ResultInfo resultInfo) {
        String basename = path.getFileName().toString();
        basename = basename.substring(0, basename.lastIndexOf('.')).toLowerCase();
        // 通过或失败
        ResultEnum surfaceResult = ResultEnum.PASSED;
        if (basename.endsWith(ResultEnum.FAILED.getSuffix())) {
            surfaceResult = ResultEnum.FAILED;
        }
        basename = basename.replace(ResultEnum.FAILED.getSuffix(), "").toLowerCase();
        // 过程或报告
        ResultEnum resultType = ResultEnum.PROCESS;
        if (basename.endsWith(ResultEnum.REPORT.getSuffix())) {
            resultType = ResultEnum.REPORT;
        }
        // 文件大小
        Long filesize = null;
        try {
            filesize = Files.size(path);
        } catch (IOException e) {
            log.error("获取文件大小失败", e);
        }
        resultInfo.setSurfaceResult(surfaceResult.name());
        resultInfo.setResultType(resultType.name());
        resultInfo.setFilesize(filesize);
        // 测试系统
        SystemEnum system = SystemEnum.ofName(resultInfo.getSystem());
        if (system == null) {
            log.error("未识别的测试系统");
        } else {
            switch (system) {
                case J750:
                    analyseForJ750(path, resultInfo);
                    break;
                case ULTRA_FLEX:
                    analyseForUltraFlex(path, resultInfo);
                    break;
                case ADV93000:
                    analyseForAdv93000(path, resultInfo);
                    break;
            }
        }
        // 入数据库时间
        resultInfo.setCreateTime(LocalDateTime.now());
    }

    public void analyseForAdv93000(Path path, ResultInfo resultInfo) {
        resultInfo.setError(false);
        ResultEnum result = ResultEnum.PASSED;
        Map<String, ResultEnum> testSuiteMap = new HashMap<>();
        boolean isReport = ResultEnum.REPORT == ResultEnum.ofName(resultInfo.getResultType()).orElse(null);
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isReport) {
                    Matcher matcher = PATTERN_LABEL_X_TESTSUITE_93000.matcher(line);
                    if (matcher.find()) {
                        String testName = matcher.group();
                        testSuiteMap.put(testName, ResultEnum.PASSED);
                        if (line.toUpperCase().contains(ResultEnum.FAILED.name())) {
                            testSuiteMap.put(testName, ResultEnum.FAILED);
                            result = ResultEnum.FAILED;
                        }
                    }
                } else {
                    Matcher matcher = PATTERN_LABEL_BEGIN_TESTSUITE_93000.matcher(line);
                    if (matcher.find()) {
                        String testName = matcher.group();
                        testSuiteMap.put(testName, ResultEnum.PASSED);
                        while (!PATTERN_LABEL_END_TESTSUITE_93000.matcher(line = reader.readLine()).find()) {
                            if (line.toUpperCase().contains(ResultEnum.FAILED.name())) {
                                testSuiteMap.put(testName, ResultEnum.FAILED);
                                result = ResultEnum.FAILED;
                                break;
                            }
                        }
                    }
                }
            }
            // 测试项
            resultInfo.setTestSuite(testSuiteMap.keySet());
            // 实际通过或失败
            resultInfo.setRealResult(result.name());
        } catch (IOException e) {
            log.error("文件读取失败", e);
            resultInfo.setError(true);
        }
    }

    public void analyseForJ750(Path path, ResultInfo resultInfo) {
        resultInfo.setError(false);
        int resultCnt = 0;
        String result = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (PATTERN_LABEL_RESULT_J750UF.matcher(line).matches()) {
                    resultCnt++;
                    reader.readLine();
                    result = reader.readLine();
                }
            }
            // 内容错误
            if (resultCnt > 1 || Strings.isEmpty(result)) {
                resultInfo.setError(true);
            }
            // 实际通过或失败
            if (result != null) {
                if (PATTERN_LABEL_PASSED_J750UF.matcher(result).matches()) {
                    resultInfo.setRealResult(ResultEnum.PASSED.name());
                } else {
                    resultInfo.setRealResult(ResultEnum.FAILED.name());
                }
            }
        } catch (IOException e) {
            log.error("文件读取失败", e);
            resultInfo.setError(true);
        }
    }

    public void analyseForUltraFlex(Path path, ResultInfo resultInfo) {
        analyseForJ750(path, resultInfo);
    }
}
