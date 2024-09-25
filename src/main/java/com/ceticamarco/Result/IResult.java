package com.ceticamarco.Result;

/**
 * Sum type for the cache data structure
 * a <i>Result</i> can either be a <i>WeatherResult</i>, <i>WindResult</i>
 * or a <i>HumidityResult</i>.
 */
public sealed interface IResult permits WeatherResult, WindResult, HumidityResult { }
