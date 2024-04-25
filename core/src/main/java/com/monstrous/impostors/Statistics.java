package com.monstrous.impostors;

public class Statistics {
    public int numTypes;
    public int lodLevels;
    public String[] typeNames;
    public int[][] instanceCount;
    public int[][] vertexCount;

    public Statistics(int numTypes, int lodLevels) {
        this.numTypes = numTypes;
        this.lodLevels = lodLevels;
        typeNames = new String[numTypes];
        instanceCount = new int[numTypes][lodLevels+1];         // +1 for impostors
        vertexCount = new int[numTypes][lodLevels+1];
    }

    public void setName(int type, String name ){
        typeNames[type] = name;
    }

    public String getName(int type ){
        return typeNames[type];
    }

    public void setVertexCount(int type, int lod, int count){
        vertexCount[type][lod] = count;
    }

    public int getVertexCount(int type, int lod){
        return vertexCount[type][lod];
    }


    public void setInstanceCount(int type, int lod, int count){
        instanceCount[type][lod] = count;
    }

    public int getInstanceCount(int type, int lod){
        return instanceCount[type][lod];
    }


}
