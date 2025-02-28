import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import redis.clients.jedis.Jedis;
import org.example.BackEndApplication;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = BackEndApplication.class)
public class RedisConnectionTest {

    @Autowired
    private Jedis jedis;

    @Test
    public void testRedisConnection() {
        String response = jedis.ping();
        System.out.println("Redis response: " + response);
        assertEquals("PONG", response);
    }
}