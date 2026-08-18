package com.utils.services.entity;

import jakarta.json.bind.annotation.JsonbProperty;

public record VitalMetricRequest(@JsonbProperty("type") String type,
                                 @JsonbProperty("value") Double value,
                                 @JsonbProperty("page_route") String pageRoute) {}
