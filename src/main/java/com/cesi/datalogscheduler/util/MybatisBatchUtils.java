package com.cesi.datalogscheduler.util;

import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.function.BiFunction;

@SuppressWarnings("unused")
@Component
@RequiredArgsConstructor
public class MybatisBatchUtils {

    /**
     * 每次处理数量
     */
    private static final int BATCH_SIZE = 50;

    private final SqlSessionFactory sqlSessionFactory;

    /**
     * 批量处理修改或者插入
     *
     * @param data        需要被处理的数据
     * @param mapperClass Mybatis的Mapper类
     * @param function    自定义处理逻辑
     * @return int 影响的总行数
     */
    public <T, U, R> int batchUpdateOrInsert(List<T> data, Class<U> mapperClass, BiFunction<T, U, R> function) {
        int i = 1;
        SqlSession batchSqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        try {
            U mapper = batchSqlSession.getMapper(mapperClass);
            int size = data.size();
            for (T element : data) {
                function.apply(element, mapper);
                if ((i % BATCH_SIZE == 0) || i == size) {
                    batchSqlSession.flushStatements();
                }
                i++;
            }
            // 非事务环境下强制提交，事务情况下该提交相当于无效
            batchSqlSession.commit(!TransactionSynchronizationManager.isSynchronizationActive());
        } catch (Exception e) {
            // 回滚
            batchSqlSession.rollback();
            throw e;
        } finally {
            batchSqlSession.close();
        }
        return i - 1;
    }
}
