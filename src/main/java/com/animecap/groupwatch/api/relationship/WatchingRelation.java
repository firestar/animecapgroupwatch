package com.animecap.groupwatch.api.relationship;

import com.animecap.groupwatch.api.models.Account;
import com.animecap.groupwatch.api.models.Episode;

/**
 * Created by Nathaniel on 3/19/2017.
 */
public class WatchingRelation {
    public Long rid;
    public float progress;
    public Account account;
    public Episode episode;
}