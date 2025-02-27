# Use Ubuntu as the base image
FROM ubuntu:18.04

# Set environment variables
ENV KAFKA_VERSION=3.8.0
ENV SCALA_VERSION=2.12
ENV FLUME_VERSION=1.11.0
ENV KAFKA_HOME=/opt/kafka
ENV FLUME_HOME=/opt/flume
ENV ZOOKEEPER_HOME=/opt/zookeeper
ENV REDIS_HOME=/opt/redis

# Install necessary dependencies
RUN apt-get update && apt-get install -y \
    openjdk-8-jdk \
    wget \
    curl \
    netcat \
    supervisor \
    redis-server && \
    apt-get clean

RUN sed -i 's/^bind 127.0.0.1/bind 0.0.0.0/' /etc/redis/redis.conf

# RUN echo "maxmemory 256mb" >> /etc/redis/redis.conf
# RUN echo "maxmemory-policy allkeys-lru" >> /etc/redis/redis.conf  # 采用 LRU 机制

ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:$PATH"

# # Download and extract Zookeeper
# RUN wget -O /tmp/zookeeper.tar.gz https://downloads.apache.org/zookeeper/zookeeper-3.7.2/apache-zookeeper-3.7.2-bin.tar.gz
# RUN tar -xzvf /tmp/zookeeper.tar.gz -C /opt
# RUN mv /opt/apache-zookeeper-3.7.2-bin /opt/zookeeper
# RUN rm -f /tmp/zookeeper.tar.gz

# RUN mkdir -p /opt/zookeeper/conf
# RUN echo "tickTime=2000 \n\
#           dataDir=/opt/zookeeper/data \n\
#           clientPort=2181 \n\
#           initLimit=5 \n\
#           syncLimit=2" > /opt/zookeeper/conf/zoo.cfg

# RUN echo "export JVMFLAGS=\"-Xms128M -Xmx256M\"" >> /opt/zookeeper/bin/zkEnv.sh

# # Download and extract Kafka
# RUN wget -O /tmp/kafka.tgz https://downloads.apache.org/kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz
# RUN tar -xzvf /tmp/kafka.tgz -C /opt
# RUN mv /opt/kafka_${SCALA_VERSION}-${KAFKA_VERSION} $KAFKA_HOME
# RUN rm -f /tmp/kafka.tgz

# RUN echo "export KAFKA_HEAP_OPTS=\"-Xmx512M -Xms256M\"" >> $KAFKA_HOME/bin/kafka-server-start.sh

# Download Apache Flume binary
RUN wget -O /tmp/flume.tar.gz https://downloads.apache.org/flume/${FLUME_VERSION}/apache-flume-${FLUME_VERSION}-bin.tar.gz
RUN tar -xzvf /tmp/flume.tar.gz -C /opt
RUN mv /opt/apache-flume-${FLUME_VERSION}-bin $FLUME_HOME
RUN rm -f /tmp/flume.tar.gz

ENV PATH="${FLUME_HOME}/bin:${PATH}"

# RUN echo "export JAVA_OPTS=\"-Xms128M -Xmx256M\"" >> $FLUME_HOME/bin/flume-ng

# Create necessary directories
RUN mkdir -p /flume/logs /kafka/logs /zookeeper/data

VOLUME [ "/log" ]

# Copy Flume configuration file
COPY flume.conf $FLUME_HOME/conf/flume.conf

# Copy Supervisor configuration
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# Copy the startup script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Expose necessary ports
EXPOSE 2181 9092 11000 6379

# # Start all services using the entrypoint script
CMD ["/entrypoint.sh"]
# CMD ["/opt/flume/bin/flume-ng", "agent", "-n", "agent", "-c", "/opt/flume/conf", "-f", "/opt/flume/conf/flume.conf"]