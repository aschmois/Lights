package com.android305.lights.util.ui;

import com.android305.lights.util.Group;

public interface UpdateableFragment {
    void update(Group group);

    int getGroupId();
}