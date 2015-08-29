package com.android305.lights.util.ui;

import com.android305.lights.util.Group;

public interface UpdateableFragment<E> {
    void update(E data);

    int getDataId();
}