package com.ceticamarco.Result;

public record WindResult(double speed,
                         String direction,
                         String icon)
        implements IResult { }