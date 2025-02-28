#!/bin/bash
set -e

# Start Zookeeper
echo "Starting Zookeeper..."
$ZOOKEEPER_HOME/bin/zkServer.sh start

# Start Kafka
echo "Starting Kafka..."
$KAFKA_HOME/bin/kafka-server-start.sh -daemon $KAFKA_HOME/config/server.properties

# Wait for Kafka to be fully initialized
echo "Waiting for Kafka to start..."
sleep 10

echo "Creating Kafka topic: MusicRecommender..."
$KAFKA_HOME/bin/kafka-topics.sh --create --if-not-exists --bootstrap-server localhost:9092 \
    --replication-factor 1 --partitions 1 --topic MusicRecommender

# Start Flume
echo "Starting Flume..."
$FLUME_HOME/bin/flume-ng agent -n agent -c $FLUME_HOME/conf -f $FLUME_HOME/conf/flume.conf &

# Start Redis
echo "Starting Redis..."
/usr/bin/redis-server /etc/redis/redis.conf &

tail -f /dev/null

# Start supervisord to manage multiple processes
# exec /usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf