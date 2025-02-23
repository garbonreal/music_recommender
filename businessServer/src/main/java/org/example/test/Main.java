package org.example.test;

import org.example.model.Music;
import org.example.service.MusicService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@ComponentScan(basePackages = "org.example")
public class Main {

    public static void main(String[] args) {
        // 初始化 Spring 上下文
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Main.class);

        // 获取 MusicService 实例
        MusicService musicService = context.getBean(MusicService.class);

        // 测试 getMusics 方法
        List<Integer> mids = Arrays.asList(9120, 9121, 9122); // 假设数据库中存在 mid 为 1, 2, 3 的音乐
        List<Music> musics = musicService.getMusics(mids);

        // 打印结果
        for (Music music : musics) {
            System.out.println("Music ID: " + music.getMid());
            System.out.println("Name: " + music.getName());
            System.out.println("URL: " + music.getUrl());
            System.out.println("-------------------");
        }

        Integer uid = 1076;

        // 测试 getMyRateMusics 方法
        List<Music> myRateMusics = musicService.getMyRateMusics(uid);

        // 打印结果
        for (int i = 0; i < Math.min(10, myRateMusics.size()); i++) {
            Music music = myRateMusics.get(i);
            System.out.println("Music ID: " + music.getMid());
            System.out.println("Name: " + music.getName());
            System.out.println("URL: " + music.getUrl());
            System.out.println("Score: " + music.getScore());
            System.out.println("-------------------");
        }

        // 关闭 Spring 上下文
        context.close();
    }
}