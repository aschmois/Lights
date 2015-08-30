package com.android305.lights.interfaces;

import com.android305.lights.util.Group;

public interface UpdateableFragment<E> {
    void update(E data);

    int getDataId();
}