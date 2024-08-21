package com.wty.secureviewkit.widgets.drag;

/**
 * @author lifawei
 * @createTime 2023/3/3 11:13
 * @describe
 */
public interface DragObject {
    // 在x轴占的长度
    int getXMaxLength();

    // 在y轴占的高
    float getYMaxLength();

    // 顺序
    int getSortIndex();
}
