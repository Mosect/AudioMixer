package com.mosect.lib.audiomixer;

import java.nio.ByteBuffer;

public interface AudioTrack {

    /**
     * 写入结果：缓存区已满，请先提交再写入
     */
    int WRITE_RESULT_FULL = -1;
    /**
     * 写入结果：缓存区已锁定，请等待解锁；一般发生在提交之后
     */
    int WRITE_RESULT_LOCKED = -2;
    /**
     * 写入结果：轨道已被删除，无法写入
     */
    int WRITE_RESULT_DELETED = -3;

    /**
     * 向音频轨道写入数据
     *
     * @param data 数据源
     * @return 实际写入字节数；大于0，表示实际写入字节数，其他请查看WRITE_RESULT_XXX字段说明
     */
    int write(ByteBuffer data);

    /**
     * 向音频轨道写入数据
     *
     * @param data   数据源
     * @param offset 偏移量
     * @param size   大小（字节）
     * @return 实际写入字节数；大于等于0，表示实际写入字节数，其他请查看WRITE_RESULT_XXX字段说明
     */
    int write(byte[] data, int offset, int size);

    /**
     * 提交数据，已锁定状态下，此操作无效；
     *
     * @return true，提交成功；false，提交失败
     */
    boolean flush();

    /**
     * 等待解锁，阻塞至解锁为止
     */
    void waitUnlock();

    /**
     * 删除轨道，此轨道不再可用
     */
    void delete();
}
