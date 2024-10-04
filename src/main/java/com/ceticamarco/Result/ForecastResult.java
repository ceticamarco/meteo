package com.ceticamarco.Result;

import com.ceticamarco.meteo.ForecastTuple;

public record ForecastResult(ForecastTuple[] forecastTuple) implements IResult {}

