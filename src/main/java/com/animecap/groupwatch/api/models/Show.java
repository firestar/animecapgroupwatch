package com.animecap.groupwatch.api.models;


import java.util.HashSet;
import java.util.Set;

/**
 * Created by Nathaniel on 3/18/2017.
 */
public class Show {
    public Long id;

    public String title, description, cover = "";
    public Integer ann, mal = 0;
    public String path;
    public Long modified = (long) 0;

    public Set<Episode> episodes = new HashSet<Episode>();

    public Show(Long modified, Long id, String title, String description, String cover, Integer ann, Integer mal, String path) {
        this.modified = modified;
        this.id = id;
        this.title = title;
        this.description = description;
        this.cover = cover;
        this.ann = ann;
        this.mal = mal;
        this.path = path;
    }

    public Show() {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public Integer getAnn() {
        return ann;
    }

    public void setAnn(Integer ann) {
        this.ann = ann;
    }

    public Integer getMal() {
        return mal;
    }

    public void setMal(Integer mal) {
        this.mal = mal;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getModified() {
        return modified;
    }

    public void setModified(Long modified) {
        this.modified = modified;
    }

    public Set<Episode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(Set<Episode> episodes) {
        this.episodes = episodes;
    }
}
