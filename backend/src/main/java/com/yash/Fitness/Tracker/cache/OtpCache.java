package com.yash.Fitness.Tracker.cache;

import com.yash.Fitness.Tracker.DTO.TempUserData;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OtpCache
{
    Map<String, TempUserData> otpData = new HashMap<>();


    public void clearCache()
    {
        otpData = new HashMap<>();
    }

    public void enterData(String email,TempUserData tempUserData)
    {
       otpData.put(email,tempUserData);
    }

    public TempUserData getData(String email)
    {
        return otpData.get(email);
    }

    public void removeData(String email)
    {
        otpData.remove(email);
    }
}
