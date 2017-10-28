package com.animecap.groupwatch.repositories;

import com.animecap.groupwatch.auth.Session;
import com.animecap.groupwatch.models.Episode;
import com.animecap.groupwatch.models.Show;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.web.client.RestTemplate;

import java.util.logging.Logger;

/**
 * Created by Nathaniel on 8/5/2017.
 */
public class AnimecapAPIService {
    @Autowired
    @LoadBalanced
    protected RestTemplate restTemplate;

    protected String serviceUrl;

    protected Logger logger = Logger.getLogger(AnimecapAPIService.class.getName());

    public AnimecapAPIService(String serviceUrl) {
        this.serviceUrl = serviceUrl.startsWith("http") ? serviceUrl : "http://" + serviceUrl;
    }

    public Show showInfo(String id){
        return restTemplate.postForObject(
            serviceUrl+"/api/show/info/"+id,
            null,
            Show.class
        );
    }
    public Episode episodeInfo(String id){
        return restTemplate.postForObject(
                serviceUrl+"/api/episode/info/"+id,
                null,
                Episode.class
        );
    }
    public Session sessionInfo(String id){
        return restTemplate.postForObject(
                serviceUrl+"/api/session/get/"+id,
                null,
                Session.class
        );
    }
}
