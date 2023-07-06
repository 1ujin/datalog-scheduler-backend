package com.cesi.datalogscheduler.entity;

import lombok.Data;
import lombok.experimental.Delegate;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Data
public class EntityList<T> implements List<T> {
    @Valid
    @Delegate
    private List<T> list = new ArrayList<>();
}
