package com.animecap.groupwatch.controller;

import com.animecap.groupwatch.auth.Session;
import com.animecap.groupwatch.auth.StoredSessions;
import com.animecap.groupwatch.messages.requests.ChatMessage;
import com.animecap.groupwatch.messages.requests.LoadVideo;
import com.animecap.groupwatch.messages.requests.SessionData;
import com.animecap.groupwatch.messages.requests.VideoPosition;
import com.animecap.groupwatch.messages.responses.GroupInfo;
import com.animecap.groupwatch.models.Episode;
import com.animecap.groupwatch.repositories.AnimecapAPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;

/**
 * Created by Nathaniel on 4/6/2017.
 */
@Controller
@Component
public class ListingController {
    @Autowired
    protected AnimecapAPIService animecapAPIService;
    @Autowired
        private SimpMessagingTemplate template;

    public ListingController(AnimecapAPIService animecapAPIService){
        this.animecapAPIService = animecapAPIService;
    }

    public HashMap<String, List<String>> groupMembers = new HashMap<>();
    public HashMap<String, Object[]> groupLeaders = new HashMap<>();
    private HashMap<String, String> sessionToGroup = new HashMap<>();

    public HashMap<String, String> groupNames = new HashMap<>();
    public TreeMap<String, String> nameToGroup = new TreeMap<>();
    public HashMap<String, Episode> currentEpisode = new HashMap<>();
    public HashMap<String, String> groupPassword = new HashMap<>();

    private TreeMap<Long, List<String>> checks = new TreeMap<>();
    private TreeMap<Long, List<String>> timeoutCheck = new TreeMap<>();
    private HashMap<String, Long> sessionToTimeoutCheck = new HashMap<>();
    //private HashMap<String, Boolean> playing = new HashMap<>();

    private HashMap<String, HashMap<String, Double>> positionInVideo = new HashMap<>();
    private HashMap<String, HashMap<String, Long>> timeToRespond = new HashMap<>();

    private HashMap<String, TreeMap<Long, List<Object[]>>> messages = new HashMap<>();

    @GetMapping("/")
    public String uptime(){
        return "server is up";
    }

    @Scheduled(fixedRate = 1000)
    private void checkActive(){
        while(checks.size()>0 && checks.firstEntry()!=null && checks.firstEntry().getKey()<=System.currentTimeMillis()){
            List<String> sessionKeys = checks.remove(checks.firstKey());
            for(String sessionKey : sessionKeys) {
                String groupId = sessionToGroup.get(sessionKey);
                if(groupId!=null) {
                    long newTime = System.currentTimeMillis() + 5000;
                    if (!timeoutCheck.containsKey(newTime)) {
                        timeoutCheck.put(newTime, new ArrayList<>()); // 4 second timeout
                    }
                    timeoutCheck.get(newTime).add(sessionKey);
                    sessionToTimeoutCheck.put(sessionKey, newTime);
                    this.template.convertAndSend("/listen/group/renew/" + sessionKey, groupId);
                }
            }
        }
    }
    public boolean alternate = false;
    @Scheduled(fixedRate = 2000)
    private void sendUpdateTimeRequest(){
        if(!alternate) {
            alternate=true;
            for (Map.Entry<String, List<String>> group : groupMembers.entrySet()) {
                for (String session : group.getValue()) {
                    this.template.convertAndSend("/listen/group/update/" + session, new Object[]{group.getKey()});
                    if(!timeToRespond.containsKey(group.getKey())){
                        timeToRespond.put(group.getKey(), new HashMap<>());
                    }
                    timeToRespond.get(group.getKey()).put(session, System.currentTimeMillis());
                }
            }
        }else {
            System.out.println(positionInVideo);
            System.out.println(groupMembers);
            System.out.println(timeToRespond);
            System.out.println(groupLeaders);
            alternate=false;
            StoredSessions ss = new StoredSessions();
            for (Map.Entry<String, List<String>> group : groupMembers.entrySet()) {
                if (groupLeaders.containsKey(group.getKey()) && positionInVideo.containsKey(group.getKey())) {
                    if (positionInVideo.get(group.getKey()).containsKey(groupLeaders.get(group.getKey())[3])) {
                        double positionLeader = positionInVideo.get(group.getKey()).get(groupLeaders.get(group.getKey())[3]);
                        for (String session : group.getValue()) {
                            if(!groupLeaders.get(group.getKey())[3].equals(session)) {
                                if (ss.contains(animecapAPIService, session) && positionInVideo.get(group.getKey()).containsKey(session)) {
                                    double position = positionInVideo.get(group.getKey()).get(session);
                                    double responseTime = 0;
                                    if (timeToRespond.containsKey(group.getKey()) && timeToRespond.get(group.getKey()).containsKey(session)) {
                                        responseTime = timeToRespond.get(group.getKey()).get(session);
                                        position += (System.currentTimeMillis() - responseTime) / 1000;
                                        positionLeader += (System.currentTimeMillis() - responseTime) / 1000;
                                    }
                                    if (Math.abs(positionLeader - position) > 2) {
                                        if (!session.equals(groupLeaders.get(group.getKey())[3])) {
                                            System.out.println("Session time update: " + session + " to: " + positionLeader + " from: " + position);
                                            this.template.convertAndSend("/listen/group/command/" + session, new Object[]{
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
        while(timeoutCheck.size()>0 && timeoutCheck.firstEntry().getKey()<=System.currentTimeMillis()){
            List<String> sessionKeys = timeoutCheck.remove(timeoutCheck.firstKey());
            if(sessionKeys!=null) {
                StoredSessions ss = new StoredSessions();
                for (String sessionKey : sessionKeys) {
                    String group = sessionToGroup.get(sessionKey);
                    if (sessionToTimeoutCheck.containsKey(sessionKey)) {
                        sessionToTimeoutCheck.remove(sessionKey);
                    }
                    //System.out.println("TimedOut: " + sessionKey);
                    clearSession(sessionKey);
                    if (ss.contains(animecapAPIService, sessionKey)) {
                        Session session = ss.get(animecapAPIService, sessionKey);
                        if(groupMembers.containsKey(group)){
                            if(groupLeaders.get(group)[3].equals(sessionKey)){
                                String newLeaderSession = groupMembers.get(group).get((int)Math.floor(Math.random()*(groupMembers.get(group).size()-1)));
                                if(ss.contains(animecapAPIService, newLeaderSession)){
                                    Session leaderSession = ss.get(animecapAPIService, newLeaderSession);
                                    groupLeaders.put(group, new Object[]{
                                            leaderSession.getAccount().id,
                                            leaderSession.getAccount().user,
                                            leaderSession.getAccount().level,
                                            newLeaderSession
                                    });
                                    groupMembers.get(group).parallelStream().forEach(s -> {
                                        this.template.convertAndSend("/listen/group/command/" + s, new Object[]{
                                                "leader",
                                                leaderSession.getAccount().id,
                                                leaderSession.getAccount().user,
                                                leaderSession.getAccount().level,
                                                newLeaderSession
                                        });
                                    });
                                }
                            }
                            groupMembers.get(group).parallelStream().forEach(s -> {
                                this.template.convertAndSend("/listen/group/command/" + s, new Object[]{
                                        "left",
                                        session.getAccount().id,
                                        session.getAccount().user,
                                        session.getAccount().level
                                });
                            });
                        }
                    }
                }
            }
        }
    }
    private void clearSession(String sessionKey){
        //System.out.println(">--------------------------------------------------<");
        String group = sessionToGroup.remove(sessionKey);
        if (groupMembers.containsKey(group)) {
            groupMembers.get(group).removeIf(s->s.equals(sessionKey));
            if(groupMembers.get(group).size()==0){
                groupMembers.remove(group);
                groupLeaders.remove(group);
                currentEpisode.remove(group);
                positionInVideo.remove(group);
                timeToRespond.remove(group);
                if(groupNames.containsKey(group)){
                    String groupName = groupNames.remove(group);
                    nameToGroup.remove(groupName);
                }
                if(messages.containsKey(group)){
                    messages.remove(group);
                }
            }
        }
        /*System.out.print("group: ");
        System.out.println(group);

        System.out.print("session: ");
        System.out.println(sessionKey);

        System.out.print("sessionToGroup: ");
        System.out.println(sessionToGroup);

        System.out.print("messages: ");
        System.out.println(messages);

        System.out.print("nameToGroup: ");
        System.out.println(nameToGroup);

        System.out.print("groupNames: ");
        System.out.println(groupNames);

        System.out.print("groupMembers: ");
        System.out.println(groupMembers);

        System.out.print("checks: ");
        System.out.println(checks);

        System.out.print("timeoutCheck: ");
        System.out.println(timeoutCheck);

        System.out.print("sessionToTimeoutCheck: ");
        System.out.println(sessionToTimeoutCheck);*/
        this.template.convertAndSend("/listen/group/listing/", nameToGroup);
    }
    private void registerSession(String sessionKey, String group){
        if(!sessionToGroup.containsKey(sessionKey)) {
            if(!groupMembers.containsKey(group)){
                groupMembers.put(group, new ArrayList<>());
            }
            if(groupMembers.get(group).parallelStream().filter(s->s.equals(sessionKey)).count()==0) {
                groupMembers.get(group).add(sessionKey);
            }
            if(!sessionToGroup.containsKey(sessionKey)) {
                sessionToGroup.put(sessionKey, group);
            }
        }
    }

    @MessageMapping("/group/chat")
    public void chatMessage(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        if(new StoredSessions().contains(animecapAPIService, message.getSession())){
            Session session = new StoredSessions().get(animecapAPIService, message.getSession());
            if(sessionToGroup.containsKey(message.getSession())) {
                String groupId = sessionToGroup.get(message.getSession());
                long time = System.currentTimeMillis();
                if (!messages.get(groupId).containsKey(time)) {
                    messages.get(groupId).put(time, new ArrayList<>());
                }
                messages.get(groupId).get(time).add(new Object[]{
                    session.getAccount().user,
                    message.getMessage()
                });
                if(groupMembers.containsKey(groupId)) {
                    groupMembers.get(groupId).parallelStream().forEach(s -> {
                        this.template.convertAndSend("/listen/group/command/" + s, new Object[]{
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

    @MessageMapping("/group/renew")
    public void sessionRenew(@Payload SessionData message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        if(new StoredSessions().contains(animecapAPIService, message.getSession())){
            Session session = new StoredSessions().get(animecapAPIService, message.getSession());
            long instancesOfSessionChecks = checks.entrySet().parallelStream().filter((e)->e.getValue().parallelStream().filter(s->s.equals(message.getSession())).count()>0).count();
            if(instancesOfSessionChecks==0) {
                long newTime = System.currentTimeMillis() + 5000;
                if (!checks.containsKey(newTime)) {
                    checks.put(newTime, new ArrayList<>());
                }
                checks.get(newTime).add(message.getSession());
            }
            if(sessionToTimeoutCheck.containsKey(message.getSession())) {
                Long timeoutKey = sessionToTimeoutCheck.remove(message.getSession());
                if(timeoutCheck.containsKey(timeoutKey)){
                    timeoutCheck.get(timeoutKey).remove(message.getSession());
                    if(timeoutCheck.get(timeoutKey).size()==0){
                        timeoutCheck.remove(timeoutKey);
                    }
                }
            }
            this.template.convertAndSend("/listen/group/renewed/"+session.getSessionKey(), new HashMap<>());
        }else{
            return;
        }
    }

    @MessageMapping("/group/register")
    public void sessionRegister(@Payload SessionData message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        StoredSessions ss = new StoredSessions();
        if(ss.contains(animecapAPIService, message.getSession())){
            Session session = ss.get(animecapAPIService, message.getSession());

            String groupId = UUID.randomUUID().toString();
            if(nameToGroup.containsKey(message.getGroup())){
                groupId = nameToGroup.get(message.getGroup());
            }else{
                nameToGroup.put(message.getGroup(), groupId);
                groupNames.put(groupId, message.getGroup());
                messages.put(groupId, new TreeMap<>());
                positionInVideo.put(groupId, new HashMap<>());
                timeToRespond.put(groupId, new HashMap<>());
                groupMembers.put(groupId, new ArrayList<>());
                currentEpisode.put(groupId, null);
                groupLeaders.put(groupId, new Object[]{
                    session.getAccount().id,
                    session.getAccount().user,
                    session.getAccount().level,
                    message.getSession()
                });
            }

            registerSession(message.getSession(), groupId);
            long newTime = System.currentTimeMillis() + 5000;
            long instancesOfSessionChecks = checks.entrySet().parallelStream().filter((e)->e.getValue().parallelStream().filter(s->s.equals(message.getSession())).count()>0).count();
            instancesOfSessionChecks+=timeoutCheck.entrySet().parallelStream().filter((e)->e.getValue().parallelStream().filter(s->s.equals(message.getSession())).count()>0).count();
            if(instancesOfSessionChecks==0) {
                if (!checks.containsKey(newTime)) {
                    checks.put(newTime, new ArrayList<>()); // 4 second timeout
                }
                checks.get(newTime).add(message.getSession());
            }
            GroupInfo gi = new GroupInfo();
            if(groupMembers.containsKey(groupId)) {
                for (String member : groupMembers.get(groupId)) {
                    if (ss.contains(animecapAPIService, member)) {
                        gi.users.add(new Object[]{
                                ss.get(animecapAPIService, member).getAccount().id,
                                ss.get(animecapAPIService, member).getAccount().user,
                                ss.get(animecapAPIService, member).getAccount().level
                        });
                    }
                }
            }
            gi.setLeader(groupLeaders.get(groupId));
            gi.setMessages(
                messages.get(groupId)
                    .descendingMap()
                    .entrySet()
                    .stream()
                    .limit(25)
                    .collect(
                        TreeMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        Map::putAll
                    )
            );
            gi.setEpisode(currentEpisode.get(groupId));
            gi.setGroup(groupId);
            if(groupMembers.containsKey(groupId)) {
                groupMembers.get(groupId).parallelStream().forEach(s -> {
                    this.template.convertAndSend("/listen/group/command/" + s, new Object[]{
                            "joined",
                            session.getAccount().id,
                            session.getAccount().user,
                            session.getAccount().level
                    });
                });
            }
            this.template.convertAndSend("/listen/group/joined/"+session.getSessionKey(), gi);
            this.template.convertAndSend("/listen/group/listing/", nameToGroup);
        }else{
            return;
        }
    }

    @MessageMapping("/group/listing")
    public void groupListing(@Payload SessionData message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        if(new StoredSessions().contains(animecapAPIService, message.getSession())){
            Session session = new StoredSessions().get(animecapAPIService, message.getSession());
            this.template.convertAndSend("/listen/group/listing/", nameToGroup);
        }else{
            return;
        }
    }

    @MessageMapping("/group/load")
    public void loadVideo(@Payload LoadVideo message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        StoredSessions ss = new StoredSessions();
        if(ss.contains(animecapAPIService, message.getSession())){
            Session session = ss.get(animecapAPIService, message.getSession());
            if(groupLeaders.containsKey(message.getGroup())){
                if(groupLeaders.get(message.getGroup())[3].equals(message.getSession())){
                    Episode e = animecapAPIService.episodeInfo(Long.toString(message.getEpisode()));
                    currentEpisode.put(message.getGroup(),e);
                    groupMembers.get(message.getGroup()).parallelStream().forEach(s -> {
                        this.template.convertAndSend("/listen/group/command/" + s, new Object[]{
                                "load",
                                e
                        });
                    });
                }
            }
        }else{
            return;
        }
    }

    @MessageMapping("/group/update")
    public void updateTime(@Payload VideoPosition message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        StoredSessions ss = new StoredSessions();
        if(ss.contains(animecapAPIService, message.getSession())){
            if(positionInVideo.containsKey(message.getGroup())) {
                positionInVideo.get(message.getGroup()).put(message.getSession(), message.getPosition());
            }
        }else{
            return;
        }
    }
    @MessageMapping("/group/play")
    public void playVideo(@Payload SessionData message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        StoredSessions ss = new StoredSessions();
        if(ss.contains(animecapAPIService, message.getSession())){
            if(groupMembers.containsKey(message.getGroup())) {
                groupMembers.get(message.getGroup()).parallelStream().forEach(s -> {
                    this.template.convertAndSend("/listen/group/command/" + s, new Object[]{
                            "play"
                    });
                });
            }
        }else{
            return;
        }
    }
    @MessageMapping("/group/pause")
    public void pauseVideo(@Payload SessionData message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        StoredSessions ss = new StoredSessions();
        if(ss.contains(animecapAPIService, message.getSession())){
            if(groupMembers.containsKey(message.getGroup())) {
                groupMembers.get(message.getGroup()).parallelStream().forEach(s -> {
                    this.template.convertAndSend("/listen/group/command/" + s, new Object[]{
                            "pause"
                    });
                });
            }
        }else{
            return;
        }
    }

    @MessageMapping("/group/leave")
    public void groupLeave(@Payload SessionData message, SimpMessageHeaderAccessor headerAccessor) throws Exception {
        StoredSessions ss = new StoredSessions();
        if(ss.contains(animecapAPIService, message.getSession())){
            Session session = ss.get(animecapAPIService, message.getSession());
            String group = null;
            if(sessionToGroup.containsKey(message.getSession())) {
                group = sessionToGroup.get(message.getSession());
            }
            clearSession(message.getSession());
            if(group!=null){
                if(groupMembers.containsKey(group)) {
                    if(groupLeaders.get(group)[3].equals(message.getSession())){
                        String newLeaderSession = groupMembers.get(group).get((int)Math.floor(Math.random()*(groupMembers.get(group).size()-1)));
                        if(ss.contains(animecapAPIService, newLeaderSession)){
                            Session leaderSession = ss.get(animecapAPIService, newLeaderSession);
                            groupLeaders.put(group, new Object[]{
                                leaderSession.getAccount().id,
                                leaderSession.getAccount().user,
                                leaderSession.getAccount().level,
                                newLeaderSession
                            });
                            groupMembers.get(group).parallelStream().forEach(s -> {
                                this.template.convertAndSend("/listen/group/command/" + s, new Object[]{
                                    "leader",
                                    leaderSession.getAccount().id,
                                    leaderSession.getAccount().user,
                                    leaderSession.getAccount().level,
                                    newLeaderSession
                                });
                            });
                        }
                    }
                    groupMembers.get(group).parallelStream().forEach(s -> {
                        this.template.convertAndSend("/listen/group/command/" + s, new Object[]{
                            "left",
                            session.getAccount().id,
                            session.getAccount().user,
                            session.getAccount().level
                        });
                    });
                }
            }
            this.template.convertAndSend("/listen/group/left/"+session.getSessionKey(), new Object[]{});
        }else{
            return;
        }
    }


}