package com.animecap.groupwatch.api.models;

import com.animecap.groupwatch.api.relationship.WatchingRelation;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;
import java.util.Set;

/**
 * Created by Nathaniel on 3/18/2017.
 */
public class Episode {

    public Episode(){

    }
    public Long id;

    @JsonIgnore
    public Show show;

    public int status, episode, version = 0;
    public String uuid;
    public float runtime = 0;
    public Date added;

    @JsonIgnore
    public Set<WatchingRelation> usersWatching;

    public void episodeFrom(Show show) {
        this.show=show;
        show.episodes.add(this);
    }

    public Long getId() {
        return id;
    }

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getEpisode() {
        return episode;
    }

    public void setEpisode(int episode) {
        this.episode = episode;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public float getRuntime() {
        return runtime;
    }

    public void setRuntime(float runtime) {
        this.runtime = runtime;
    }

    public Date getAdded() {
        return added;
    }

    public void setAdded(Date added) {
        this.added = added;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
