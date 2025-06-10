package org.example.rest;

import org.example.service.*;
import org.example.utils.Constant;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;


@RequestMapping("/rest")
@Controller
public class MusicRestApi {

//    private Logger logger = LoggerFactory.getLogger(MovieRestApi.class);

    private static Logger logger = Logger.getLogger(MusicRestApi.class.getName());

    @Autowired
    private MusicService musicService;

    /**
     * get music by uid
     * @param username
     * @param model
     * @return
     */
    @RequestMapping(value = "/music", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getMusics(@RequestParam("uid")Integer uid) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("history_music", musicService.getHistoryMusics(uid));
        result.put("recommand_music", musicService.getRealTimeMusics(uid));
        result.put("popular_music", musicService.getPopularMusics(uid));
        return result;
    }


        
    @RequestMapping(value = "/status", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getCalculationStatus(@RequestParam("uid") Integer uid) {
        Map<String, Object> result = new HashMap<>();
        String status = musicService.getCalculationStatus(uid);
        result.put("success", true);
        result.put("status", status != null ? status : "done"); // 如果 Redis 为空，默认返回 "done"
        return result;
    }


    @RequestMapping(value = "/click", produces = "application/json", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> clickToMusic(@RequestParam("uid")int uid, @RequestParam("mid")int mid) {
        boolean complete = musicService.musicListening(uid, mid);
        Map<String, Object> result = new HashMap<>();
        if(complete) {
            System.out.print("=========complete=========");
            logger.info(Constant.MUSIC_LISTENING_PREFIX + ":" + uid + "|" + mid + "|" + System.currentTimeMillis()/1000);
            result.put("success", true);
            result.put("message","click success");
        }
        else {
            result.put("success", false);
            result.put("message","click failed");
        }
        return result;
    }
}