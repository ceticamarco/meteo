package com.ceticamarco.Result;

public final class WindResult implements IResult {
    public double speed;
    public String direction;
    public String icon;

    public WindResult(double speed, String direction, String icon) {
        this.speed = speed;
        this.direction = direction;
        this.icon = icon;
    }
}
