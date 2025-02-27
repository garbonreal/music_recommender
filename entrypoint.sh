#!/bin/bash
set -e

# # Start Zookeeper
# echo "Starting Zookeeper..."
# $ZOOKEEPER_HOME/bin/zkServer.sh start

# # Start Kafka
# echo "Starting Kafka..."
# $KAFKA_HOME/bin/kafka-server-start.sh -daemon $KAFKA_HOME/config/server.properties

# # Wait for Kafka to be fully initialized
# echo "Waiting for Kafka to start..."
# sleep 10

# Start Flume
echo "Starting Flume..."
$FLUME_HOME/bin/flume-ng agent -n agent -c $FLUME_HOME/conf -f $FLUME_HOME/conf/flume.conf &

# Start Redis
echo "Starting Redis..."
redis-server /etc/redis/redis.conf &

# Start supervisord to manage multiple processes
exec /usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf