package org.example.rest;

import org.example.model.Music;
import org.example.service.*;
import org.example.utils.Constant;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Random;

import java.util.List;

@RequestMapping("/rest/music")
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
    @RequestMapping(value = "/recommand", produces = "application/json", method = RequestMethod.GET )
    @ResponseBody
    public Map<String, Object> getMyRateMovies(@RequestParam("uid")Integer uid, Model model) {
        // http://localhost:8080/rest/music/myrate?uid=1076
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("rating_music", musicService.getMyRateMusics(uid));
        return result;
    }
}