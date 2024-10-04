package com.ceticamarco.Result;

import org.springframework.http.MediaType;

public record ApiResult(String result, MediaType mediaType) { }
