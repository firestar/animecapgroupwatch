package com.animecap.groupwatch.controller;

import com.animecap.groupwatch.api.auth.Session;
import com.animecap.groupwatch.api.auth.StoredSessions;
import com.animecap.groupwatch.api.repositories.AnimecapAPIService;
import com.animecap.groupwatch.messages.responses.GroupInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Nathaniel on 10/29/2017.
 */
@Controller
@Component
public class Schedules {
    @Autowired
    protected GroupData groupData;
    @Autowired
    protected AnimecapAPIService animecapAPIService;

    @Autowired
    private SimpMessagingTemplate template;

    public Schedules(GroupData groupData, AnimecapAPIService animecapAPIService){
        this.groupData = groupData;
        this.animecapAPIService = animecapAPIService;
    }


    @Scheduled(fixedRate = 1000)
    private void checkActive(){
        while(groupData.getChecks().size()>0 && groupData.getChecks().firstEntry()!=null && groupData.getChecks().firstEntry().getKey()<=System.currentTimeMillis()){
            List<String> sessionKeys = groupData.getChecks().remove(groupData.getChecks().firstKey());
            for(String sessionKey : sessionKeys) {
                String groupId = groupData.getSessionToGroup().get(sessionKey);
                if(groupId!=null) {
                    long newTime = System.currentTimeMillis() + 5000;
                    if (!groupData.getTimeoutCheck().containsKey(newTime)) {
                        groupData.getTimeoutCheck().put(newTime, new ArrayList<>()); // 4 second timeout
                    }
                    groupData.getTimeoutCheck().get(newTime).add(sessionKey);
                    groupData.getSessionToTimeoutCheck().put(sessionKey, newTime);
                    this.template.convertAndSend("/listen/renew/" + sessionKey, groupId);
                }
            }
        }
    }
    public boolean alternate = false;
    @Scheduled(fixedRate = 2000)
    private void sendUpdateTimeRequest(){
        if(!alternate) {
            alternate=true;
            for (Map.Entry<String, List<String>> group : groupData.groupMembers.entrySet()) {
                for (String session : group.getValue()) {
                    this.template.convertAndSend("/listen/update/" + session, new Object[]{group.getKey()});
                    if(!groupData.getTimeToRespond().containsKey(group.getKey())){
                        groupData.getTimeToRespond().put(group.getKey(), new HashMap<>());
                    }
                    groupData.getTimeToRespond().get(group.getKey()).put(session, System.currentTimeMillis());
                }
            }
        }else {
            alternate=false;
            StoredSessions ss = new StoredSessions();
            for (Map.Entry<String, List<String>> group : groupData.getGroupMembers().entrySet()) {
                if (groupData.getGroupLeaders().containsKey(group.getKey()) && groupData.getPositionInVideo().containsKey(group.getKey())) {
                    if (groupData.getPositionInVideo().get(group.getKey()).containsKey(groupData.getGroupLeaders().get(group.getKey())[3])) {
                        double positionLeader = groupData.getPositionInVideo().get(group.getKey()).get(groupData.getGroupLeaders().get(group.getKey())[3]);
                        for (String session : group.getValue()) {
                            if(!groupData.getGroupLeaders().get(group.getKey())[3].equals(session)) {
                                if (ss.contains(animecapAPIService, session) && groupData.getPositionInVideo().get(group.getKey()).containsKey(session)) {
                                    double position = groupData.getPositionInVideo().get(group.getKey()).get(session);
                                    double responseTime = 0;
                                    if (groupData.getTimeToRespond().containsKey(group.getKey()) && groupData.getTimeToRespond().get(group.getKey()).containsKey(session)) {
                                        responseTime = groupData.getTimeToRespond().get(group.getKey()).get(session);
                                        position += (System.currentTimeMillis() - responseTime) / 1000;
                                        positionLeader += (System.currentTimeMillis() - responseTime) / 1000;
                                    }
                                    if (Math.abs(positionLeader - position) > 1) {
                                        if (!session.equals(groupData.getGroupLeaders().get(group.getKey())[3])) {
                                            System.out.println("Session time update: " + session + " to: " + positionLeader + " from: " + position);
                                            this.template.convertAndSend("/listen/command/" + session, new Object[]{
                                                    "seek",
                                                    group.getKey(),
                                                    positionLeader // adjust for lag
                                            });
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    @Scheduled(fixedRate = 1000)
    private void clearInnactive(){
        while(groupData.getTimeoutCheck().size()>0 && groupData.getTimeoutCheck().firstEntry().getKey()<=System.currentTimeMillis()){
            List<String> sessionKeys = groupData.getTimeoutCheck().remove(groupData.getTimeoutCheck().firstKey());
            if(sessionKeys!=null) {
                StoredSessions ss = new StoredSessions();
                for (String sessionKey : sessionKeys) {
                    String group = groupData.getSessionToGroup().get(sessionKey);
                    GroupInfo groupInfo = groupData.getGroupById(group);
                    if (groupData.getSessionToTimeoutCheck().containsKey(sessionKey)) {
                        groupData.getSessionToTimeoutCheck().remove(sessionKey);
                    }
                    //System.out.println("TimedOut: " + sessionKey);
                    groupData.clearSession(sessionKey);
                    if (ss.contains(animecapAPIService, sessionKey)) {
                        Session session = ss.get(animecapAPIService, sessionKey);
                        if(groupData.getGroupMembers().containsKey(group)){
                            groupData.getGroupMembers().get(group).remove(sessionKey);
                            if(groupData.getGroupLeaders().get(group)[3].equals(sessionKey)){
                                String newLeaderSession = groupData.getGroupMembers().get(group).get((int)Math.floor(Math.random()*(groupData.getGroupMembers().get(group).size()-1)));
                                if(ss.contains(animecapAPIService, newLeaderSession)){
                                    Session leaderSession = ss.get(animecapAPIService, newLeaderSession);

                                    Object[] tmpLeader = new Object[]{
                                            leaderSession.getAccount().id,
                                            leaderSession.getAccount().user,
                                            leaderSession.getAccount().level,
                                            newLeaderSession
                                    };
                                    groupData.getGroupLeaders().put(group, tmpLeader);
                                    if(groupInfo!=null) groupInfo.setLeader(tmpLeader);
                                    groupData.getGroupMembers().get(group).parallelStream().forEach(s -> {
                                        this.template.convertAndSend("/listen/command/" + s, new Object[]{
                                                "leader",
                                                leaderSession.getAccount().id,
                                                leaderSession.getAccount().user,
                                                leaderSession.getAccount().level,
                                                newLeaderSession
                                        });
                                    });
                                }
                            }
                            groupData.getGroupMembers().get(group).parallelStream().forEach(s -> {
                                this.template.convertAndSend("/listen/command/" + s, new Object[]{
                                        "left",
                                        session.getAccount().id,
                                        session.getAccount().user,
                                        session.getAccount().level
                                });
                            });
                            if(groupInfo!=null) groupData.updateMemberList(groupInfo, ss);
                            if(groupInfo!=null) this.template.convertAndSend("/listen/listing/", groupData.getGroups());
                        }
                    }
                }
            }
        }
    }
}