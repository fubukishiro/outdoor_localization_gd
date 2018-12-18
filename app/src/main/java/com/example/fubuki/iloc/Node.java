/*该类表示平面上点的集合，包含由集合计算节点位置的方法*/
package com.example.fubuki.iloc;

import java.util.ArrayList;

public class Node {
    public ArrayList<Point> nodePointArray;
    public Node()
    {
        nodePointArray = new ArrayList<>();
    }
    public int getSize()
    {
        return  nodePointArray.size();
    }
    public Point getPoint(int i)
    {
        return nodePointArray.get(i);
    }
    public void clear()
    {
        nodePointArray.clear();
    }
    public void addPoint(Point pPoint)
    {
        nodePointArray.add(pPoint);
    }

}
