package com.animecap.groupwatch.controller;

import com.animecap.groupwatch.api.auth.Session;
import com.animecap.groupwatch.api.auth.StoredSessions;
import com.animecap.groupwatch.api.models.Episode;
import com.animecap.groupwatch.api.models.Show;
import com.animecap.groupwatch.api.repositories.AnimecapAPIService;
import com.animecap.groupwatch.messages.requests.ChatMessage;
import com.animecap.groupwatch.messages.requests.LoadVideo;
import com.animecap.groupwatch.messages.requests.SessionData;
import com.animecap.groupwatch.messages.requests.VideoPosition;
import com.animecap.groupwatch.messages.responses.GroupInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Created by Nathaniel on 10/29/2017.
 */
@Controller
@Component
public class MessageController {

    @Autowired
    protected GroupData groupData;
    @Autowired
    protected AnimecapAPIService animecapAPIService;

    @Autowired
    private SimpMessagingTemplate template;

    public MessageController(GroupData groupData, AnimecapAPIService animecapAPIService){
        this.groupData = groupData;
        this.animecapAPIService = animecapAPIService;
    }

    @MessageMapping("/chat")
    public void chatMessage(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        if(new StoredSessions().contains(animecapAPIService, message.getSession())){
            Session session = new StoredSessions().get(animecapAPIService, message.getSession());
            if(groupData.getSessionToGroup().containsKey(message.getSession())) {
                String groupId = groupData.getSessionToGroup().get(message.getSession());
                GroupInfo groupInfo = groupData.getGroupById(groupId);
                long time = System.currentTimeMillis();
                if (!groupData.getMessages().get(groupId).containsKey(time)) {
                    groupData.getMessages().get(groupId).put(time, new ArrayList<>());
                }
                groupData.getMessages().get(groupId).get(time).add(new Object[]{
                        session.getAccount().user,
                        message.getMessage()
                });
                if(groupInfo!=null) {
                    groupData.updateGroupMessages(groupInfo);
                }
                if(groupData.getGroupMembers().containsKey(groupId)) {
                    groupData.getGroupMembers().get(groupId).parallelStream().forEach(s -> {
                        this.template.convertAndSend("/listen/command/" + s, new Object[]{
                                "message",
                                time,
                                session.getAccount().user,
                                message.getMessage()
                        });
                    });
                }
            }
        }else{
            return;
        }
    }

    @MessageMapping("/renew")
    public void sessionRenew(@Payload SessionData message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        if(new StoredSessions().contains(animecapAPIService, message.getSession())){
            Session session = new StoredSessions().get(animecapAPIService, message.getSession());
            long instancesOfSessionChecks = groupData.getChecks().entrySet().parallelStream().filter((e)->e.getValue().parallelStream().filter(s->s.equals(message.getSession())).count()>0).count();
            if(instancesOfSessionChecks==0) {
                long newTime = System.currentTimeMillis() + 5000;
                if (!groupData.getChecks().containsKey(newTime)) {
                    groupData.getChecks().put(newTime, new ArrayList<>());
                }
                groupData.getChecks().get(newTime).add(message.getSession());
            }
            if(groupData.getSessionToTimeoutCheck().containsKey(message.getSession())) {
                Long timeoutKey = groupData.getSessionToTimeoutCheck().remove(message.getSession());
                if(groupData.getTimeoutCheck().containsKey(timeoutKey)){
                    groupData.getTimeoutCheck().get(timeoutKey).remove(message.getSession());
                    if(groupData.getTimeoutCheck().get(timeoutKey).size()==0){
                        groupData.getTimeoutCheck().remove(timeoutKey);
                    }
                }
            }
            this.template.convertAndSend("/listen/renewed/"+session.getSessionKey(), new HashMap<>());
        }else{
            return;
        }
    }

    @MessageMapping("/register")
    public void sessionRegister(@Payload SessionData message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        StoredSessions ss = new StoredSessions();
        if(ss.contains(animecapAPIService, message.getSession())){
            Session session = ss.get(animecapAPIService, message.getSession());
            GroupInfo groupInfo;
            String groupId = UUID.randomUUID().toString();
            if(groupData.getNameToGroup().containsKey(message.getGroup())){
                groupId = groupData.getNameToGroup().get(message.getGroup());
                groupInfo = groupData.getGroupById(groupId);
            }else{
                groupInfo = groupData.createGroupByName(groupId, message.getGroup());
                groupData.getNameToGroup().put(message.getGroup(), groupId);
                groupData.getGroupNames().put(groupId, message.getGroup());
                groupData.getMessages().put(groupId, new TreeMap<>());
                groupData.getPositionInVideo().put(groupId, new HashMap<>());
                groupData.getTimeToRespond().put(groupId, new HashMap<>());
                groupData.getGroupMembers().put(groupId, new ArrayList<>());
                groupData.getCurrentEpisode().put(groupId, null);
                Object[] tmp = new Object[]{
                        session.getAccount().id,
                        session.getAccount().user,
                        session.getAccount().level,
                        message.getSession()
                };
                groupData.getGroupLeaders().put(groupId, tmp);
                groupInfo.setLeader(tmp);
            }

            groupData.registerSession(message.getSession(), groupId);
            long newTime = System.currentTimeMillis() + 5000;
            long instancesOfSessionChecks = groupData.getChecks().entrySet().parallelStream().filter((e)->e.getValue().parallelStream().filter(s->s.equals(message.getSession())).count()>0).count();
            instancesOfSessionChecks+=groupData.getTimeoutCheck().entrySet().parallelStream().filter((e)->e.getValue().parallelStream().filter(s->s.equals(message.getSession())).count()>0).count();
            if(instancesOfSessionChecks==0) {
                if (!groupData.getChecks().containsKey(newTime)) {
                    groupData.getChecks().put(newTime, new ArrayList<>()); // 4 second timeout
                }
                groupData.getChecks().get(newTime).add(message.getSession());
            }


            // disabled as this is done on message submission
            //updateGroupMessages(groupInfo);

            groupData.updateMemberList(groupInfo, ss);

            //disabled prefer setting this in load event
            //groupInfo.setEpisode(groupData.getCurrentEpisode().get(groupId));
            if(groupData.getGroupMembers().containsKey(groupId)) {
                groupData.getGroupMembers().get(groupId).parallelStream().forEach(s -> {
                    this.template.convertAndSend("/listen/command/" + s, new Object[]{
                            "joined",
                            session.getAccount().id,
                            session.getAccount().user,
                            session.getAccount().level
                    });
                });
            }
            this.template.convertAndSend("/listen/joined/"+session.getSessionKey(), groupInfo);
            this.template.convertAndSend("/listen/listing/", groupData.getGroups());
        }else{
            return;
        }
    }

    @MessageMapping("/listing")
    public void groupListing(@Payload SessionData message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        StoredSessions ss = new StoredSessions();
        if(ss.contains(animecapAPIService, message.getSession())){
            Session session = ss.get(animecapAPIService, message.getSession());
            this.template.convertAndSend("/listen/listing/", groupData.getGroups());
        }else{
            return;
        }
    }

    @MessageMapping("/load")
    public void loadVideo(@Payload LoadVideo message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        StoredSessions ss = new StoredSessions();
        if(ss.contains(animecapAPIService, message.getSession())){
            Session session = ss.get(animecapAPIService, message.getSession());
            if(groupData.getGroupLeaders().containsKey(message.getGroup())){
                if(groupData.getGroupLeaders().get(message.getGroup())[3].equals(message.getSession())){
                    GroupInfo groupInfo = groupData.getGroupById(message.getGroup());
                    Episode e = animecapAPIService.episodeInfo(Long.toString(message.getEpisode()));
                    groupData.getCurrentEpisode().put(message.getGroup(),e);
                    groupInfo.setEpisode(e);
                    System.out.println(e.showId);
                    Show show = animecapAPIService.showInfo(Long.toString(e.showId));
                    groupInfo.setShow(show);
                    groupData.getGroupMembers().get(message.getGroup()).parallelStream().forEach(s -> {
                        this.template.convertAndSend("/listen/command/" + s, new Object[]{
                                "load",
                                e
                        });
                    });
                    this.template.convertAndSend("/listen/listing/", groupData.getGroups());
                }
            }
        }else{
            return;
        }
    }

    @MessageMapping("/update")
    public void updateTime(@Payload VideoPosition message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        StoredSessions ss = new StoredSessions();
        if(ss.contains(animecapAPIService, message.getSession())){
            if(groupData.getPositionInVideo().containsKey(message.getGroup())) {
                groupData.getPositionInVideo().get(message.getGroup()).put(message.getSession(), message.getPosition());
            }
        }else{
            return;
        }
    }
    @MessageMapping("/play")
    public void playVideo(@Payload SessionData message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        StoredSessions ss = new StoredSessions();
        if(ss.contains(animecapAPIService, message.getSession())){
            if(groupData.getGroupMembers().containsKey(message.getGroup())) {
                GroupInfo groupInfo = groupData.getGroupById(message.getGroup());
                if(groupInfo!=null) groupInfo.status = "play";
                groupData.getGroupMembers().get(message.getGroup()).parallelStream().forEach(s -> {
                    this.template.convertAndSend("/listen/command/" + s, new Object[]{
                            "play"
                    });
                });
                this.template.convertAndSend("/listen/listing/", groupData.getGroups());
            }
        }else{
            return;
        }
    }
    @MessageMapping("/pause")
    public void pauseVideo(@Payload SessionData message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        StoredSessions ss = new StoredSessions();
        if(ss.contains(animecapAPIService, message.getSession())){
            if(groupData.getGroupMembers().containsKey(message.getGroup())) {
                GroupInfo groupInfo = groupData.getGroupById(message.getGroup());
                if(groupInfo!=null) groupInfo.status = "pause";
                groupData.getGroupMembers().get(message.getGroup()).parallelStream().forEach(s -> {
                    this.template.convertAndSend("/listen/command/" + s, new Object[]{
                            "pause"
                    });
                });
                this.template.convertAndSend("/listen/listing/", groupData.getGroups());
            }
        }else{
            return;
        }
    }

    @MessageMapping("/leave")
    public void groupLeave(@Payload SessionData message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        StoredSessions ss = new StoredSessions();
        if(ss.contains(animecapAPIService, message.getSession())){
            Session session = ss.get(animecapAPIService, message.getSession());
            String group = null;
            if(groupData.getSessionToGroup().containsKey(message.getSession())) {
                group = groupData.getSessionToGroup().get(message.getSession());
            }
            groupData.clearSession(message.getSession());
            if(group!=null){
                if(groupData.getGroupMembers().containsKey(group)) {
                    GroupInfo groupInfo = groupData.getGroupById(group);
                    if(groupData.getGroupLeaders().get(group)[3].equals(message.getSession())){
                        String newLeaderSession = groupData.getGroupMembers().get(group).get((int)Math.floor(Math.random()*(groupData.getGroupMembers().get(group).size()-1)));
                        if(ss.contains(animecapAPIService, newLeaderSession)){
                            Session leaderSession = ss.get(animecapAPIService, newLeaderSession);
                            Object[] tmp = new Object[]{
                                    leaderSession.getAccount().id,
                                    leaderSession.getAccount().user,
                                    leaderSession.getAccount().level,
                                    newLeaderSession
                            };
                            groupData.getGroupLeaders().put(group, tmp);
                            if(groupInfo!=null) groupInfo.leader = tmp;
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
                    if(groupInfo!=null) groupData.updateMemberList(groupInfo,ss);
                    this.template.convertAndSend("/listen/listing/", groupData.getGroups());
                }
            }
            this.template.convertAndSend("/listen/left/"+session.getSessionKey(), new Object[]{});
        }else{
            return;
        }
    }
}
