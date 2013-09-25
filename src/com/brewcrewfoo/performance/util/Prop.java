package com.brewcrewfoo.performance.util;

/**
 * Created by h0rn3t on 22.09.2013.
 */
public class Prop {

    private String name;
    private String data;

    public Prop(String n,String d){
        name = n;
        data = d;
    }
    public String getName(){
        return name;
    }
    public void setName(String d){
        this.name=d;

    }
    public String getVal(){
        return data;
    }
    public void setVal(String d){
        this.data=d;
    }

}
