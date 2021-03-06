package com.animecap.groupwatch.messages.responses;

import com.animecap.groupwatch.api.models.Episode;
import com.animecap.groupwatch.api.models.Show;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by Nathaniel on 4/8/2017.
 */
public class GroupInfo {
    public List<Object[]> users = new ArrayList<>();
    public String group=null;
    public String name=null;
    public String status="pause";
    public Episode episode=null;
    public Show show=null;
    public Object[] leader = new Object[]{};
    public TreeMap<Long, List<Object[]>> messages = new TreeMap<>();

    public GroupInfo(){}

    public void setUsers(List<Object[]> users) {
        this.users = users;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setMessages(TreeMap<Long, List<Object[]>> messages) {
        this.messages = messages;
    }

    public void setLeader(Object[] leader) {
        this.leader = leader;
    }

    public void setEpisode(Episode episode) {
        this.episode = episode;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setShow(Show show) {
        this.show = show;
    }
}
