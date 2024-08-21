package com.wty.secureviewkit.widgets.drag;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lifawei
 * @createTime 2023/4/3 8:44
 * @describe
 */
public class DragData {
    private final static AtomicInteger idFactory = new AtomicInteger(0);
    private int id;
    private DragObject object;
    private float yValue;
    private int xMaxValue;
    private int xValue;
    private int xPosition;

    private DragData(DragData data) {
        this.id = data.id;
        this.object = data.object;
        this.yValue = data.yValue;
        this.xMaxValue = data.xMaxValue;
        this.xValue = data.xValue;
        this.xPosition = data.xPosition;
    }

    public DragData(DragObject object) {
        this.object = object;
        id = idFactory.getAndAdd(1);
        yValue = object.getYMaxLength();
        xMaxValue = object.getXMaxLength();
        xPosition = 0;
    }

    private boolean isValid() {
        return object != null;
    }

    public DragObject getObject() {
        return object;
    }

    public float getYValue() {
        return object.getYMaxLength();
    }

    public int getXMaxValue() {
        return xMaxValue;
    }

    public int getXValue() {
        return xValue;
    }

    public void setXValue(int xValue) {
        this.xValue = Math.max(0, xValue);
    }

    public int getXPosition() {
        return xPosition;
    }

    public void setXPosition(int xPosition) {
        this.xPosition = Math.max(0, xPosition);
    }

    public DragData copy() {
        return new DragData(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DragData)) {
            return false;
        }
        DragData data = (DragData)o;
        return id == data.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DragData{" + "yValue=" + yValue + ", xMaxValue=" + xMaxValue + ", xValue=" + xValue + ", xPosition="
            + xPosition + '}';
    }
}
