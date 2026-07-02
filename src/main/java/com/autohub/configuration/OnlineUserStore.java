package com.autohub.configuration;


import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OnlineUserStore {

    private final Set<String> users =
            ConcurrentHashMap.newKeySet();

    public void add(String user){
        users.add(user);
    }

    public void remove(String user){
        users.remove(user);
    }

    public boolean isOnline(String user){
        return users.contains(user);
    }

    public Set<String> getUsers(){
        return users;
    }
}
