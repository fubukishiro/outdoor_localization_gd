package com.example.fubuki.iloc;

/*计算点类，该仅表示坐标系中的一个点*/
public class Point {
    private double x;
    private double y;
    private boolean side;
    Point(double px,double py,boolean pSide)
    {
        x = px;
        y = py;
        side = pSide;
    }
    Point(double px,double py)
    {
        x = px;
        y = py;
    }

    public double getX()
    {
        return x;
    }
    public double getY()
    {
        return y;
    }
}

