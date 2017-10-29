package com.animecap.groupwatch.controller;

import com.animecap.groupwatch.api.auth.Session;
import com.animecap.groupwatch.api.auth.StoredSessions;
import com.animecap.groupwatch.api.models.Show;
import com.animecap.groupwatch.messages.requests.ChatMessage;
import com.animecap.groupwatch.messages.requests.LoadVideo;
import com.animecap.groupwatch.messages.requests.SessionData;
import com.animecap.groupwatch.messages.requests.VideoPosition;
import com.animecap.groupwatch.messages.responses.GroupInfo;
import com.animecap.groupwatch.api.models.Episode;
import com.animecap.groupwatch.api.repositories.AnimecapAPIService;
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
public class GroupData {
    @Autowired
    protected AnimecapAPIService animecapAPIService;
    @Autowired
        private SimpMessagingTemplate template;

    public GroupData(AnimecapAPIService animecapAPIService){
        this.animecapAPIService = animecapAPIService;
    }

    public HashMap<String, List<String>> groupMembers = new HashMap<>();
    public HashMap<String, Object[]> groupLeaders = new HashMap<>();
    private HashMap<String, String> sessionToGroup = new HashMap<>();

    public TreeMap<String, GroupInfo> groups = new TreeMap<>();
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

    public GroupInfo getGroupByName(String groupName){
        if(groups.size()==0){
            return null;
        }
        if(!nameToGroup.containsKey(groupName)){
            return null;
        }
        if(groups.containsKey(nameToGroup.get(groupName))){
            return groups.get(nameToGroup.get(groupName));
        }
        return null;
    }
    public GroupInfo getGroupById(String groupId){
        if(groups.size()==0){
            return null;
        }
        if(groups.containsKey(groupId)){
            return groups.get(groupId);
        }
        return null;
    }
    public GroupInfo createGroupByName(String groupId, String groupName){
        if(!groups.containsKey(groupId)){
            GroupInfo groupInfo = new GroupInfo();
            groupInfo.group = groupId;
            groupInfo.name = groupName;
            groups.put(groupId, groupInfo);
            return groupInfo;
        }
        return null;
    }
    public void removeGroupInfo(String groupId){
        if(groups.size()==0){
            return;
        }
        groups.remove(groupId);
    }
    public void updateMemberList(GroupInfo groupInfo, StoredSessions ss){
        if(groupMembers.containsKey(groupInfo.group)) {
            groupInfo.users = new ArrayList<>();
            for (String member : groupMembers.get(groupInfo.group)) {
                if (ss.contains(animecapAPIService, member)) {
                    Session memberSession = ss.get(animecapAPIService, member);
                    groupInfo.users.add(new Object[]{
                            memberSession.getAccount().id,
                            memberSession.getAccount().user,
                            memberSession.getAccount().level
                    });
                }
            }
        }
    }
    public void updateGroupMessages(GroupInfo groupInfo){
        groupInfo.setMessages(
            messages.get(groupInfo.group)
                .descendingMap()
                .entrySet()
                .stream()
                .limit(15)
                .collect(
                    TreeMap::new,
                    (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                    Map::putAll
                )
        );
    }

    public void clearSession(String sessionKey){
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
                int numberOfGroups = groups.size();
                removeGroupInfo(group);
                if(numberOfGroups>groups.size()) { // was one removed?
                    this.template.convertAndSend("/listen/listing/", groups);
                }
            }
        }
    }
    public void registerSession(String sessionKey, String group){
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

    public AnimecapAPIService getAnimecapAPIService() {
        return animecapAPIService;
    }

    public void setAnimecapAPIService(AnimecapAPIService animecapAPIService) {
        this.animecapAPIService = animecapAPIService;
    }

    public SimpMessagingTemplate getTemplate() {
        return template;
    }

    public void setTemplate(SimpMessagingTemplate template) {
        this.template = template;
    }

    public HashMap<String, List<String>> getGroupMembers() {
        return groupMembers;
    }

    public void setGroupMembers(HashMap<String, List<String>> groupMembers) {
        this.groupMembers = groupMembers;
    }

    public HashMap<String, Object[]> getGroupLeaders() {
        return groupLeaders;
    }

    public void setGroupLeaders(HashMap<String, Object[]> groupLeaders) {
        this.groupLeaders = groupLeaders;
    }

    public HashMap<String, String> getSessionToGroup() {
        return sessionToGroup;
    }

    public void setSessionToGroup(HashMap<String, String> sessionToGroup) {
        this.sessionToGroup = sessionToGroup;
    }

    public TreeMap<String, GroupInfo> getGroups() {
        return groups;
    }

    public void setGroups(TreeMap<String, GroupInfo> groups) {
        this.groups = groups;
    }

    public HashMap<String, String> getGroupNames() {
        return groupNames;
    }

    public void setGroupNames(HashMap<String, String> groupNames) {
        this.groupNames = groupNames;
    }

    public TreeMap<String, String> getNameToGroup() {
        return nameToGroup;
    }

    public void setNameToGroup(TreeMap<String, String> nameToGroup) {
        this.nameToGroup = nameToGroup;
    }

    public HashMap<String, Episode> getCurrentEpisode() {
        return currentEpisode;
    }

    public void setCurrentEpisode(HashMap<String, Episode> currentEpisode) {
        this.currentEpisode = currentEpisode;
    }

    public HashMap<String, String> getGroupPassword() {
        return groupPassword;
    }

    public void setGroupPassword(HashMap<String, String> groupPassword) {
        this.groupPassword = groupPassword;
    }

    public TreeMap<Long, List<String>> getChecks() {
        return checks;
    }

    public void setChecks(TreeMap<Long, List<String>> checks) {
        this.checks = checks;
    }

    public TreeMap<Long, List<String>> getTimeoutCheck() {
        return timeoutCheck;
    }

    public void setTimeoutCheck(TreeMap<Long, List<String>> timeoutCheck) {
        this.timeoutCheck = timeoutCheck;
    }

    public HashMap<String, Long> getSessionToTimeoutCheck() {
        return sessionToTimeoutCheck;
    }

    public void setSessionToTimeoutCheck(HashMap<String, Long> sessionToTimeoutCheck) {
        this.sessionToTimeoutCheck = sessionToTimeoutCheck;
    }

    public HashMap<String, HashMap<String, Double>> getPositionInVideo() {
        return positionInVideo;
    }

    public void setPositionInVideo(HashMap<String, HashMap<String, Double>> positionInVideo) {
        this.positionInVideo = positionInVideo;
    }

    public HashMap<String, HashMap<String, Long>> getTimeToRespond() {
        return timeToRespond;
    }

    public void setTimeToRespond(HashMap<String, HashMap<String, Long>> timeToRespond) {
        this.timeToRespond = timeToRespond;
    }

    public HashMap<String, TreeMap<Long, List<Object[]>>> getMessages() {
        return messages;
    }

    public void setMessages(HashMap<String, TreeMap<Long, List<Object[]>>> messages) {
        this.messages = messages;
    }
}
